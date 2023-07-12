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
import org.stellar.anchor.api.rpc.action.DoStellarRefundRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.config.CustodyConfig
import org.stellar.anchor.custody.CustodyService
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.platform.data.JdbcSep24Transaction
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
  }

  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore
  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore
  @MockK(relaxed = true) private lateinit var validator: Validator
  @MockK(relaxed = true) private lateinit var horizon: Horizon
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
        validator,
        custodyConfig,
        horizon,
        assetService,
        custodyService
      )
  }

  @Test
  fun test_handle_unsupportedProtocol() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    val spyTxn24 = spyk(txn24)

    every { txn24Store.findByTransactionId(TX_ID) } returns spyTxn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { spyTxn24.protocol } returns "100"

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals("Protocol[100] is not supported by action[do_stellar_refund]", ex.message)
  }

  @Test
  fun test_handle_unsupportedKind() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.INCOMPLETE.toString()
    txn24.kind = PlatformTransactionData.Kind.DEPOSIT.kind

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals("Action[do_stellar_refund] is not supported for status[incomplete]", ex.message)
  }

  @Test
  fun test_handle_unsupportedStatus() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.INCOMPLETE.toString()
    txn24.kind = PlatformTransactionData.Kind.WITHDRAWAL.kind

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals("Action[do_stellar_refund] is not supported for status[incomplete]", ex.message)
  }

  @Test
  fun test_handle_invalidRequest() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    txn24.transferReceivedAt = Instant.now()
    txn24.kind = PlatformTransactionData.Kind.WITHDRAWAL.kind

    val violation1: ConstraintViolation<DoStellarRefundRequest> = mockk()
    val violation2: ConstraintViolation<DoStellarRefundRequest> = mockk()

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
  fun test_handle_disabledCustodyIntegration() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.transferReceivedAt = Instant.now()
    txn24.kind = PlatformTransactionData.Kind.WITHDRAWAL.kind
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
        .memo("testMemo")
        .memoType("text")
        .refund(DoStellarRefundRequest.Refund.builder().amount("-1").amountFee("-0.1").build())
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.transferReceivedAt = Instant.now()
    txn24.kind = PlatformTransactionData.Kind.WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    var ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("refund.amount.amount should be positive", ex.message)
    request.refund.amount = "1"

    ex = assertThrows { handler.handle(request) }
    assertEquals("refund.amountFee.amount should be positive", ex.message)
    request.refund.amountFee = "1"
  }

  @Test
  fun test_handle_invalidMemo() {
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .memo("testMemo")
        .memoType("hash")
        .refund(DoStellarRefundRequest.Refund.builder().amount("1").amountFee("0.1").build())
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.transferReceivedAt = Instant.now()
    txn24.kind = PlatformTransactionData.Kind.WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("Invalid memo or memo_type: bytes must be 32-bytes long.", ex.message)
  }

  @Test
  fun test_handle_ok() {
    val transferReceivedAt = Instant.now()
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .memo("testMemo")
        .memoType("text")
        .refund(DoStellarRefundRequest.Refund.builder().amount("1").amountFee("0.1").build())
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = SepTransactionStatus.PENDING_ANCHOR.toString()
    txn24.kind = PlatformTransactionData.Kind.WITHDRAWAL.kind
    txn24.transferReceivedAt = transferReceivedAt
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountIn = "1"
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountOut = "1"
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.amountFee = "0.1"
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
    expectedSep24Txn.kind = PlatformTransactionData.Kind.WITHDRAWAL.kind
    expectedSep24Txn.status = SepTransactionStatus.PENDING_STELLAR.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.requestAssetCode = FIAT_USD_CODE
    expectedSep24Txn.amountInAsset = STELLAR_USDC
    expectedSep24Txn.amountIn = "1"
    expectedSep24Txn.amountOutAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "1"
    expectedSep24Txn.amountFeeAsset = STELLAR_USDC
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.refundMemo = request.memo
    expectedSep24Txn.refundMemoType = request.memoType
    expectedSep24Txn.transferReceivedAt = transferReceivedAt

    JSONAssert.assertEquals(
      GSON.toJson(expectedSep24Txn),
      GSON.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = PlatformTransactionData.Sep.SEP_24
    expectedResponse.kind = PlatformTransactionData.Kind.WITHDRAWAL
    expectedResponse.status = SepTransactionStatus.PENDING_STELLAR
    expectedResponse.amountExpected = Amount(null, FIAT_USD)
    expectedResponse.amountIn = Amount("1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("1", STELLAR_USDC)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.transferReceivedAt = transferReceivedAt

    JSONAssert.assertEquals(
      GSON.toJson(expectedResponse),
      GSON.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedSep24Txn.updatedAt >= startDate)
    assertTrue(expectedSep24Txn.updatedAt <= endDate)
  }
}
