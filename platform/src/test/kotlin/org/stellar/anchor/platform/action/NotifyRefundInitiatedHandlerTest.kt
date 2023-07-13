package org.stellar.anchor.platform.action

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.time.Instant
import javax.validation.ConstraintViolation
import javax.validation.Validator
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.exception.rpc.InvalidParamsException
import org.stellar.anchor.api.exception.rpc.InvalidRequestException
import org.stellar.anchor.api.platform.GetTransactionResponse
import org.stellar.anchor.api.platform.PlatformTransactionData
import org.stellar.anchor.api.rpc.action.NotifyRefundInitiatedRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.RefundPayment
import org.stellar.anchor.api.shared.Refunds
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.platform.data.JdbcSep24RefundPayment
import org.stellar.anchor.platform.data.JdbcSep24Refunds
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.util.GsonUtils

class NotifyRefundInitiatedHandlerTest {
  companion object {
    private val GSON = GsonUtils.getInstance()
    private const val TX_ID = "testId"
    private const val FIAT_USD = "iso4217:USD"
    private const val STELLAR_USDC =
      "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"
    private const val FIAT_USD_CODE = "USD"
  }

  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore
  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore
  @MockK(relaxed = true) private lateinit var validator: Validator
  @MockK(relaxed = true) private lateinit var horizon: Horizon
  @MockK(relaxed = true) private lateinit var assetService: AssetService

  private lateinit var handler: NotifyRefundInitiatedHandler

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    this.assetService = DefaultAssetService.fromJsonResource("test_assets.json")
    this.handler =
      NotifyRefundInitiatedHandler(txn24Store, txn31Store, validator, horizon, assetService)
  }

  @Test
  fun test_handle_unsupportedProtocol() {
    val request = NotifyRefundInitiatedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    val spyTxn24 = spyk(txn24)

    every { txn24Store.findByTransactionId(TX_ID) } returns spyTxn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { spyTxn24.protocol } returns "100"

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals("Protocol[100] is not supported by action[notify_refund_initiated]", ex.message)
  }

  @Test
  fun test_handle_unsupportedKind() {
    val request = NotifyRefundInitiatedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    txn24.kind = PlatformTransactionData.Kind.WITHDRAWAL.kind
    txn24.transferReceivedAt = Instant.now()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[notify_refund_initiated] is not supported for status[pending_anchor]",
      ex.message
    )
  }

  @Test
  fun test_handle_unsupportedStatus() {
    val request = NotifyRefundInitiatedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.INCOMPLETE.toString()
    txn24.kind = PlatformTransactionData.Kind.DEPOSIT.kind

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[notify_refund_initiated] is not supported for status[incomplete]",
      ex.message
    )
  }

  @Test
  fun test_handle_invalidRequest() {
    val request = NotifyRefundInitiatedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    txn24.kind = PlatformTransactionData.Kind.DEPOSIT.kind
    txn24.transferReceivedAt = Instant.now()

    val violation1: ConstraintViolation<NotifyRefundInitiatedRequest> = mockk()
    val violation2: ConstraintViolation<NotifyRefundInitiatedRequest> = mockk()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { violation1.message } returns "violation error message 1"
    every { violation2.message } returns "violation error message 2"
    every { validator.validate(request) } returns setOf(violation1, violation2)

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals(
      "violation error message 1\n" + "violation error message 2",
      ex.message?.trimIndent()
    )
  }

  @Test
  fun test_handle_invalidAmounts() {
    val request =
      NotifyRefundInitiatedRequest.builder()
        .transactionId(TX_ID)
        .refund(
          NotifyRefundInitiatedRequest.Refund.builder()
            .amount("-1")
            .amountFee("-0.1")
            .id("1")
            .build()
        )
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.transferReceivedAt = Instant.now()
    txn24.kind = PlatformTransactionData.Kind.DEPOSIT.kind

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    var ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("refund.amount.amount should be positive", ex.message)
    request.refund.amount = "1"

    ex = assertThrows { handler.handle(request) }
    assertEquals("refund.amountFee.amount should be non-negative", ex.message)
  }

  @Test
  fun test_handle_ok_first_refund() {
    val transferReceivedAt = Instant.now()
    val request =
      NotifyRefundInitiatedRequest.builder()
        .transactionId(TX_ID)
        .refund(
          NotifyRefundInitiatedRequest.Refund.builder().amount("1").amountFee("0.1").id("1").build()
        )
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    txn24.kind = PlatformTransactionData.Kind.DEPOSIT.kind
    txn24.transferReceivedAt = transferReceivedAt
    txn24.amountInAsset = STELLAR_USDC
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountIn = "1"
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountOut = "1"
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.amountFee = "0.1"

    val sep24TxnCapture = slot<JdbcSep24Transaction>()
    val payment = JdbcSep24RefundPayment()
    payment.id = request.refund.id
    payment.amount = request.refund.amount
    payment.fee = request.refund.amountFee

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.kind = PlatformTransactionData.Kind.DEPOSIT.kind
    expectedSep24Txn.status = SepTransactionStatus.PENDING_EXTERNAL.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.requestAssetCode = FIAT_USD_CODE
    expectedSep24Txn.amountIn = "1"
    expectedSep24Txn.amountInAsset = STELLAR_USDC
    expectedSep24Txn.amountOutAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "1"
    expectedSep24Txn.amountFeeAsset = STELLAR_USDC
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.transferReceivedAt = transferReceivedAt
    val expectedRefunds = JdbcSep24Refunds()
    expectedRefunds.amountRefunded = "1"
    expectedRefunds.amountFee = "0.1"
    expectedRefunds.payments = listOf(payment)
    expectedSep24Txn.refunds = expectedRefunds

    JSONAssert.assertEquals(
      GSON.toJson(expectedSep24Txn),
      GSON.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = PlatformTransactionData.Sep.SEP_24
    expectedResponse.kind = PlatformTransactionData.Kind.DEPOSIT
    expectedResponse.status = SepTransactionStatus.PENDING_EXTERNAL
    expectedResponse.amountExpected = Amount(null, FIAT_USD)
    expectedResponse.amountIn = Amount("1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("1", STELLAR_USDC)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.transferReceivedAt = transferReceivedAt
    val refundPayment = RefundPayment()
    refundPayment.amount = Amount("1", txn24.amountInAsset)
    refundPayment.fee = Amount("0.1", txn24.amountInAsset)
    refundPayment.id = request.refund.id
    refundPayment.idType = RefundPayment.IdType.STELLAR
    val refunded = Amount("1", txn24.amountInAsset)
    val refundedFee = Amount("0.1", txn24.amountInAsset)
    expectedResponse.refunds = Refunds(refunded, refundedFee, arrayOf(refundPayment))

    JSONAssert.assertEquals(
      GSON.toJson(expectedResponse),
      GSON.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedSep24Txn.updatedAt >= startDate)
    assertTrue(expectedSep24Txn.updatedAt <= endDate)
  }

  @Test
  fun test_handle_ok_second_refund() {
    val transferReceivedAt = Instant.now()
    val request =
      NotifyRefundInitiatedRequest.builder()
        .transactionId(TX_ID)
        .refund(
          NotifyRefundInitiatedRequest.Refund.builder().amount("1").amountFee("0.1").id("1").build()
        )
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    txn24.kind = PlatformTransactionData.Kind.DEPOSIT.kind
    txn24.transferReceivedAt = transferReceivedAt
    txn24.amountInAsset = STELLAR_USDC
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountIn = "1"
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountOut = "1"
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.amountFee = "0.1"

    val sep24TxnCapture = slot<JdbcSep24Transaction>()
    val payment = JdbcSep24RefundPayment()
    payment.id = request.refund.id
    payment.amount = request.refund.amount
    payment.fee = request.refund.amountFee
    val refunds = JdbcSep24Refunds()
    refunds.amountRefunded = "1"
    refunds.amountFee = "0.1"
    refunds.payments = listOf(payment)
    txn24.refunds = refunds

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.kind = PlatformTransactionData.Kind.DEPOSIT.kind
    expectedSep24Txn.status = SepTransactionStatus.PENDING_EXTERNAL.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.requestAssetCode = FIAT_USD_CODE
    expectedSep24Txn.amountIn = "1"
    expectedSep24Txn.amountInAsset = STELLAR_USDC
    expectedSep24Txn.amountOutAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "1"
    expectedSep24Txn.amountFeeAsset = STELLAR_USDC
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.transferReceivedAt = transferReceivedAt
    val expectedRefunds = JdbcSep24Refunds()
    expectedRefunds.amountRefunded = "2"
    expectedRefunds.amountFee = "0.2"
    expectedRefunds.payments = listOf(payment, payment)
    expectedSep24Txn.refunds = expectedRefunds

    JSONAssert.assertEquals(
      GSON.toJson(expectedSep24Txn),
      GSON.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = PlatformTransactionData.Sep.SEP_24
    expectedResponse.kind = PlatformTransactionData.Kind.DEPOSIT
    expectedResponse.status = SepTransactionStatus.PENDING_EXTERNAL
    expectedResponse.amountExpected = Amount(null, FIAT_USD)
    expectedResponse.amountIn = Amount("1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("1", STELLAR_USDC)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.transferReceivedAt = transferReceivedAt
    val refundPayment = RefundPayment()
    refundPayment.amount = Amount("1", txn24.amountInAsset)
    refundPayment.fee = Amount("0.1", txn24.amountInAsset)
    refundPayment.id = request.refund.id
    refundPayment.idType = RefundPayment.IdType.STELLAR
    val refunded = Amount("2", txn24.amountInAsset)
    val refundedFee = Amount("0.2", txn24.amountInAsset)
    expectedResponse.refunds = Refunds(refunded, refundedFee, arrayOf(refundPayment, refundPayment))

    JSONAssert.assertEquals(
      GSON.toJson(expectedResponse),
      GSON.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedSep24Txn.updatedAt >= startDate)
    assertTrue(expectedSep24Txn.updatedAt <= endDate)
  }
}
