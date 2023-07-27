package org.stellar.anchor.platform.action

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
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
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL
import org.stellar.anchor.api.rpc.action.AmountAssetRequest
import org.stellar.anchor.api.rpc.action.DoStellarRefundRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE
import org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.config.CustodyConfig
import org.stellar.anchor.custody.CustodyService
import org.stellar.anchor.platform.data.JdbcSep24Transaction
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
    every { spyTxn24.protocol } returns "38"

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[do_stellar_refund] is not supported. Status[pending_anchor], kind[null], protocol[38], funds received[false]",
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
      "Action[do_stellar_refund] is not supported. Status[incomplete], kind[deposit], protocol[24], funds received[false]",
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
      "Action[do_stellar_refund] is not supported. Status[incomplete], kind[withdrawal], protocol[24], funds received[false]",
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
    every { requestValidator.validate(request) } throws InvalidParamsException("Invalid request")

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("Invalid request", ex.message?.trimIndent())
  }

  @Test
  fun test_handle_disabledCustodyIntegration() {
    val request = DoStellarRefundRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountFeeAsset = FIAT_USD
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
            .amount(AmountAssetRequest("1", STELLAR_USDC))
            .amountFee(AmountAssetRequest("0.1", FIAT_USD))
            .build()
        )
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountFeeAsset = FIAT_USD
    txn24.transferReceivedAt = Instant.now()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    request.refund.amount.amount = "-1"
    var ex = assertThrows<BadRequestException> { handler.handle(request) }
    assertEquals("refund.amount.amount should be positive", ex.message)
    request.refund.amount.amount = "1"

    request.refund.amountFee.amount = "-0.1"
    ex = assertThrows { handler.handle(request) }
    assertEquals("refund.amountFee.amount should be non-negative", ex.message)
  }

  @Test
  fun test_handle_invalidAssets() {
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .refund(
          DoStellarRefundRequest.Refund.builder()
            .amount(AmountAssetRequest("1", STELLAR_USDC))
            .amountFee(AmountAssetRequest("0.1", FIAT_USD))
            .build()
        )
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountFeeAsset = FIAT_USD
    txn24.transferReceivedAt = Instant.now()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true

    request.refund.amount.asset = FIAT_USD
    var ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("refund.amount.asset does not match transaction amount_in_asset", ex.message)
    request.refund.amount.asset = STELLAR_USDC

    request.refund.amountFee.asset = STELLAR_USDC
    ex = assertThrows { handler.handle(request) }
    assertEquals(
      "refund.amount_fee.asset does not match match transaction amount_fee_asset",
      ex.message
    )
  }

  @Test
  fun test_handle_ok() {
    val transferReceivedAt = Instant.now()
    val request =
      DoStellarRefundRequest.builder()
        .transactionId(TX_ID)
        .refund(
          DoStellarRefundRequest.Refund.builder()
            .amount(AmountAssetRequest("1", STELLAR_USDC))
            .amountFee(AmountAssetRequest("0.1", FIAT_USD))
            .build()
        )
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.transferReceivedAt = transferReceivedAt
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountIn = "1"
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountOut = "1"
    txn24.amountFeeAsset = FIAT_USD
    txn24.amountFee = "0.1"
    txn24.refundMemo = "memo"
    txn24.refundMemoType = "text"
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
    expectedSep24Txn.amountIn = "1"
    expectedSep24Txn.amountOutAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "1"
    expectedSep24Txn.amountFeeAsset = FIAT_USD
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.refundMemo = "memo"
    expectedSep24Txn.refundMemoType = "text"
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
    expectedResponse.amountIn = Amount("1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("1", STELLAR_USDC)
    expectedResponse.amountFee = Amount("0.1", FIAT_USD)
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
