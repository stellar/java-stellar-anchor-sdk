package org.stellar.anchor.platform.action

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.rpc.InvalidParamsException
import org.stellar.anchor.api.exception.rpc.InvalidRequestException
import org.stellar.anchor.api.platform.GetTransactionResponse
import org.stellar.anchor.api.platform.PlatformTransactionData
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind.RECEIVE
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_38
import org.stellar.anchor.api.rpc.action.AmountRequest
import org.stellar.anchor.api.rpc.action.DoStellarRefundRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE
import org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR
import org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_RECEIVER
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.Customers
import org.stellar.anchor.api.shared.StellarId
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.config.CustodyConfig
import org.stellar.anchor.custody.CustodyService
import org.stellar.anchor.platform.data.JdbcSep24RefundPayment
import org.stellar.anchor.platform.data.JdbcSep24Refunds
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.platform.data.JdbcSep31RefundPayment
import org.stellar.anchor.platform.data.JdbcSep31Refunds
import org.stellar.anchor.platform.data.JdbcSep31Transaction
import org.stellar.anchor.platform.validator.RequestValidator
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.util.GsonUtils

class DoStellarRefundHandlerTest {

  companion object {
    private val GSON = GsonUtils.getInstance()
    private const val TX_ID = "testId"
    private const val FIAT_USD = "iso4217:USD"
    private const val STELLAR_USDC =
      "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"
    private const val FIAT_USD_CODE = "USD"
    private const val MEMO = "testMemo"
    private const val MEMO_TYPE = "text"
    private const val VALIDATION_ERROR_MESSAGE = "Invalid request"
  }

  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore

  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore

  @MockK(relaxed = true) private lateinit var requestValidator: RequestValidator

  @MockK(relaxed = true) private lateinit var assetService: AssetService

  @MockK(relaxed = true) private lateinit var custodyConfig: CustodyConfig

  @MockK(relaxed = true) private lateinit var custodyService: CustodyService

  private lateinit var handler: DoStellarRefundHandler

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    this.assetService = DefaultAssetService.fromJsonResource("test_assets.json")
    this.handler =
      DoStellarRefundHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        custodyConfig,
        assetService,
        custodyService
      )
  }

  @Test
  fun test_handle_unsupportedProtocol() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    val spyTxn24 = spyk(txn24)

    every { txn24Store.findByTransactionId(TX_ID) } returns spyTxn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { spyTxn24.protocol } returns SEP_38.sep.toString()

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[do_stellar_refund] is not supported for status[pending_anchor], kind[null] and protocol[38]",
      ex.message
    )
  }

  @Test
  fun test_handle_unsupportedKind() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = DEPOSIT.kind

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[do_stellar_refund] is not supported for status[incomplete], kind[deposit] and protocol[24]",
      ex.message
    )
  }

  @Test
  fun test_handle_unsupportedStatus() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[do_stellar_refund] is not supported for status[incomplete], kind[withdrawal] and protocol[24]",
      ex.message
    )
  }

  @Test
  fun test_handle_invalidRequest() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.transferReceivedAt = Instant.now()
    txn24.kind = WITHDRAWAL.kind

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { requestValidator.validate(request) } throws
      InvalidParamsException(VALIDATION_ERROR_MESSAGE)

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals(VALIDATION_ERROR_MESSAGE, ex.message?.trimIndent())
  }

  @Test
  fun test_handle_disabledCustodyIntegration() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.transferReceivedAt = Instant.now()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("Action[do_stellar_refund] requires enabled custody integration", ex.message)
  }

  @Test
  fun test_handle_invalidAmounts() {
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .refund(
          DoStellarRefundRequest.Refund.builder()
            .amount(AmountRequest("-1"))
            .amountFee(AmountRequest("-0.1"))
            .build()
        )
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.transferReceivedAt = Instant.now()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    var ex = assertThrows<BadRequestException> { handler.handle(request) }
    assertEquals("refund.amount.amount should be positive", ex.message)
    request.refund.amount = AmountRequest("1")

    ex = assertThrows { handler.handle(request) }
    assertEquals("refund.amountFee.amount should be non-negative", ex.message)
  }

  @Test
  fun test_handle_sep24_ok() {
    val transferReceivedAt = Instant.now()
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .refund(
          DoStellarRefundRequest.Refund.builder()
            .amount(AmountRequest("1"))
            .amountFee(AmountRequest("0.1"))
            .build()
        )
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.transferReceivedAt = transferReceivedAt
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountIn = "1.1"
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountOut = "1"
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.amountFee = "0.1"
    txn24.refundMemo = MEMO
    txn24.refundMemoType = MEMO_TYPE
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.kind = WITHDRAWAL.kind
    expectedSep24Txn.status = SepTransactionStatus.PENDING_STELLAR.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.requestAssetCode = FIAT_USD_CODE
    expectedSep24Txn.amountInAsset = STELLAR_USDC
    expectedSep24Txn.amountIn = "1.1"
    expectedSep24Txn.amountOutAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "1"
    expectedSep24Txn.amountFeeAsset = STELLAR_USDC
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.refundMemo = MEMO
    expectedSep24Txn.refundMemoType = MEMO_TYPE
    expectedSep24Txn.transferReceivedAt = transferReceivedAt

    JSONAssert.assertEquals(
      GSON.toJson(expectedSep24Txn),
      GSON.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = PlatformTransactionData.Sep.SEP_24
    expectedResponse.kind = WITHDRAWAL
    expectedResponse.status = SepTransactionStatus.PENDING_STELLAR
    expectedResponse.amountExpected = Amount(null, FIAT_USD)
    expectedResponse.amountIn = Amount("1.1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("1", STELLAR_USDC)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.transferReceivedAt = transferReceivedAt

    JSONAssert.assertEquals(
      GSON.toJson(expectedResponse),
      GSON.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(sep24TxnCapture.captured.updatedAt >= startDate)
    assertTrue(sep24TxnCapture.captured.updatedAt <= endDate)
  }

  @Test
  fun test_handle_sep31_ok() {
    val transferReceivedAt = Instant.now()
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .refund(
          DoStellarRefundRequest.Refund.builder()
            .amount(AmountRequest("1"))
            .amountFee(AmountRequest("0.1"))
            .build()
        )
        .build()
    val txn31 = JdbcSep31Transaction()
    txn31.status = PENDING_RECEIVER.toString()
    txn31.transferReceivedAt = transferReceivedAt
    txn31.amountInAsset = STELLAR_USDC
    txn31.amountIn = "1.1"
    txn31.amountOutAsset = STELLAR_USDC
    txn31.amountOut = "1"
    txn31.amountFeeAsset = STELLAR_USDC
    txn31.amountFee = "0.1"
    val sep31TxnCapture = slot<JdbcSep31Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns txn31
    every { txn31Store.save(capture(sep31TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn24Store.save(any()) }

    val expectedSep31Txn = JdbcSep31Transaction()
    expectedSep31Txn.status = SepTransactionStatus.PENDING_STELLAR.toString()
    expectedSep31Txn.updatedAt = sep31TxnCapture.captured.updatedAt
    expectedSep31Txn.amountInAsset = STELLAR_USDC
    expectedSep31Txn.amountIn = "1.1"
    expectedSep31Txn.amountOutAsset = STELLAR_USDC
    expectedSep31Txn.amountOut = "1"
    expectedSep31Txn.amountFeeAsset = STELLAR_USDC
    expectedSep31Txn.amountFee = "0.1"
    expectedSep31Txn.transferReceivedAt = transferReceivedAt

    JSONAssert.assertEquals(
      GSON.toJson(expectedSep31Txn),
      GSON.toJson(sep31TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = PlatformTransactionData.Sep.SEP_31
    expectedResponse.kind = RECEIVE
    expectedResponse.status = SepTransactionStatus.PENDING_STELLAR
    expectedResponse.amountExpected = Amount(null, STELLAR_USDC)
    expectedResponse.amountIn = Amount("1.1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("1", STELLAR_USDC)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.updatedAt = sep31TxnCapture.captured.updatedAt
    expectedResponse.transferReceivedAt = transferReceivedAt
    expectedResponse.customers = Customers(StellarId(null, null), StellarId(null, null))

    JSONAssert.assertEquals(
      GSON.toJson(expectedResponse),
      GSON.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(sep31TxnCapture.captured.updatedAt >= startDate)
    assertTrue(sep31TxnCapture.captured.updatedAt <= endDate)
  }

  @Test
  fun test_handle_sep24_sent_more_then_amount_in() {
    val transferReceivedAt = Instant.now()
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .refund(
          DoStellarRefundRequest.Refund.builder()
            .amount(AmountRequest("1"))
            .amountFee(AmountRequest("0.1"))
            .build()
        )
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.transferReceivedAt = transferReceivedAt
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountIn = "1.1"
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountOut = "1"
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.amountFee = "0.1"
    txn24.refundMemo = MEMO
    txn24.refundMemoType = MEMO_TYPE

    val payment = JdbcSep24RefundPayment()
    payment.id = "1"
    payment.amount = "0.1"
    payment.fee = "0"
    val refunds = JdbcSep24Refunds()
    refunds.amountRefunded = "1"
    refunds.amountFee = "0.1"
    refunds.payments = listOf(payment)
    txn24.refunds = refunds

    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(TX_ID) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("Refund amount exceeds amount_in", ex.message)
  }

  @Test
  fun test_handle_sep31_sent_more_then_amount_in() {
    val transferReceivedAt = Instant.now()
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .refund(
          DoStellarRefundRequest.Refund.builder()
            .amount(AmountRequest("1"))
            .amountFee(AmountRequest("0.1"))
            .build()
        )
        .build()
    val txn31 = JdbcSep31Transaction()
    txn31.status = PENDING_RECEIVER.toString()
    txn31.transferReceivedAt = transferReceivedAt
    txn31.amountInAsset = STELLAR_USDC
    txn31.amountIn = "1"
    txn31.amountOutAsset = STELLAR_USDC
    txn31.amountOut = "1"
    txn31.amountFeeAsset = STELLAR_USDC
    txn31.amountFee = "0.1"
    val sep31TxnCapture = slot<JdbcSep31Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns txn31
    every { txn31Store.save(capture(sep31TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("Refund amount exceeds amount_in", ex.message)
  }

  @Test
  fun test_handle_sep31_sent_less_then_amount_in() {
    val transferReceivedAt = Instant.now()
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .refund(
          DoStellarRefundRequest.Refund.builder()
            .amount(AmountRequest("0.8"))
            .amountFee(AmountRequest("0.1"))
            .build()
        )
        .build()
    val txn31 = JdbcSep31Transaction()
    txn31.status = PENDING_RECEIVER.toString()
    txn31.transferReceivedAt = transferReceivedAt
    txn31.amountInAsset = STELLAR_USDC
    txn31.amountIn = "1"
    txn31.amountOutAsset = STELLAR_USDC
    txn31.amountOut = "1"
    txn31.amountFeeAsset = STELLAR_USDC
    txn31.amountFee = "0.1"
    val sep31TxnCapture = slot<JdbcSep31Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns txn31
    every { txn31Store.save(capture(sep31TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("Refund amount is less than amount_in", ex.message)
  }

  @Test
  fun test_handle_sep31_sent_multiple_refund() {
    val transferReceivedAt = Instant.now()
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .refund(
          DoStellarRefundRequest.Refund.builder()
            .amount(AmountRequest("0.8"))
            .amountFee(AmountRequest("0.1"))
            .build()
        )
        .build()
    val txn31 = JdbcSep31Transaction()
    txn31.status = PENDING_RECEIVER.toString()
    txn31.transferReceivedAt = transferReceivedAt
    txn31.amountInAsset = STELLAR_USDC
    txn31.amountIn = "1"
    txn31.amountOutAsset = STELLAR_USDC
    txn31.amountOut = "1"
    txn31.amountFeeAsset = STELLAR_USDC
    txn31.amountFee = "0.1"
    val payment = JdbcSep31RefundPayment()
    payment.id = "1"
    payment.amount = "0.1"
    payment.fee = "0.1"
    val refunds = JdbcSep31Refunds()
    refunds.amountRefunded = "0.1"
    refunds.amountFee = "0.1"
    refunds.payments = listOf(payment)
    txn31.refunds = refunds
    val sep31TxnCapture = slot<JdbcSep31Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns txn31
    every { txn31Store.save(capture(sep31TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Multiple refunds aren't supported for kind[RECEIVE], protocol[31] and action[do_stellar_refund]",
      ex.message
    )
  }
}
