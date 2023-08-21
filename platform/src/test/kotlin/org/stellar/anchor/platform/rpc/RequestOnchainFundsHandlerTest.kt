package org.stellar.anchor.platform.rpc

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
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
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.api.event.AnchorEvent.Type.TRANSACTION_STATUS_CHANGED
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.rpc.InvalidParamsException
import org.stellar.anchor.api.exception.rpc.InvalidRequestException
import org.stellar.anchor.api.platform.GetTransactionResponse
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_38
import org.stellar.anchor.api.rpc.method.AmountAssetRequest
import org.stellar.anchor.api.rpc.method.AmountRequest
import org.stellar.anchor.api.rpc.method.RequestOnchainFundsRequest
import org.stellar.anchor.api.sep.SepTransactionStatus.*
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.SepDepositInfo
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.config.CustodyConfig
import org.stellar.anchor.config.CustodyConfig.CustodyType.FIREBLOCKS
import org.stellar.anchor.config.CustodyConfig.CustodyType.NONE
import org.stellar.anchor.custody.CustodyService
import org.stellar.anchor.event.EventService
import org.stellar.anchor.event.EventService.EventQueue.TRANSACTION
import org.stellar.anchor.event.EventService.Session
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.platform.service.Sep24DepositInfoNoneGenerator
import org.stellar.anchor.platform.service.Sep24DepositInfoSelfGenerator
import org.stellar.anchor.platform.validator.RequestValidator
import org.stellar.anchor.sep24.Sep24Transaction
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.util.GsonUtils

class RequestOnchainFundsHandlerTest {

  companion object {
    private val gson = GsonUtils.getInstance()
    private const val TX_ID = "testId"
    private const val FIAT_USD = "iso4217:USD"
    private const val STELLAR_USDC =
      "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"
    private const val STELLAR_USDC_CODE = "USDC"
    private const val STELLAR_USDC_ISSUER =
      "GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"
    private const val TEXT_MEMO = "testMemo"
    private const val TEXT_MEMO_2 = "testMemo2"
    private const val HASH_MEMO = "YWYwOTk2M2QtNzU3Mi00NGQ4LWE5MDktMmY2YzMzNTY="
    private const val TEXT_MEMO_TYPE = "text"
    private const val HASH_MEMO_TYPE = "hash"
    private const val INVALID_MEMO_TYPE = "invalidMemoType"
    private const val DESTINATION_ACCOUNT = "testDestinationAccount"
    private const val DESTINATION_ACCOUNT_2 = "testDestinationAccount2"
    private const val VALIDATION_ERROR_MESSAGE = "Invalid request"
  }

  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore

  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore

  @MockK(relaxed = true) private lateinit var requestValidator: RequestValidator

  @MockK(relaxed = true) private lateinit var assetService: AssetService

  @MockK(relaxed = true) private lateinit var custodyConfig: CustodyConfig

  @MockK(relaxed = true) private lateinit var custodyService: CustodyService

  @MockK(relaxed = true)
  private lateinit var sep24DepositInfoGenerator: Sep24DepositInfoNoneGenerator

  @MockK(relaxed = true) private lateinit var eventService: EventService

  @MockK(relaxed = true) private lateinit var eventSession: Session

  @MockK(relaxed = true) private lateinit var sepTransactionCounter: Counter

  private lateinit var handler: RequestOnchainFundsHandler

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { eventService.createSession(any(), TRANSACTION) } returns eventSession
    this.assetService = DefaultAssetService.fromJsonResource("test_assets.json")
    this.handler =
      RequestOnchainFundsHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        custodyService,
        custodyConfig,
        sep24DepositInfoGenerator,
        eventService
      )
  }

  @Test
  fun test_handle_unsupportedProtocol() {
    val request = RequestOnchainFundsRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = DEPOSIT.kind
    val spyTxn24 = spyk(txn24)

    every { txn24Store.findByTransactionId(TX_ID) } returns spyTxn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { spyTxn24.protocol } returns SEP_38.sep.toString()

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "RPC method[request_onchain_funds] is not supported. Status[incomplete], kind[null], protocol[38], funds received[false]",
      ex.message
    )

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_unsupportedStatus() {
    val request = RequestOnchainFundsRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_TRUST.toString()
    txn24.kind = WITHDRAWAL.kind

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "RPC method[request_onchain_funds] is not supported. Status[pending_trust], kind[withdrawal], protocol[24], funds received[false]",
      ex.message
    )

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_transferReceived() {
    val request = RequestOnchainFundsRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.transferReceivedAt = Instant.now()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "RPC method[request_onchain_funds] is not supported. Status[pending_anchor], kind[withdrawal], protocol[24], funds received[true]",
      ex.message
    )

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_unsupportedKind() {
    val request = RequestOnchainFundsRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = DEPOSIT.kind

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "RPC method[request_onchain_funds] is not supported. Status[incomplete], kind[deposit], protocol[24], funds received[false]",
      ex.message
    )

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_invalidRequest() {
    val request = RequestOnchainFundsRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { requestValidator.validate(request) } throws
      InvalidParamsException(VALIDATION_ERROR_MESSAGE)

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals(VALIDATION_ERROR_MESSAGE, ex.message?.trimIndent())

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_ok_withExpectedAmount() {
    val request =
      RequestOnchainFundsRequest.builder()
        .transactionId(TX_ID)
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("0.9", FIAT_USD))
        .amountFee(AmountAssetRequest("0.1", STELLAR_USDC))
        .amountExpected(AmountRequest("1"))
        .memo(TEXT_MEMO)
        .memoType(TEXT_MEMO_TYPE)
        .destinationAccount(DESTINATION_ACCOUNT)
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.requestAssetCode = STELLAR_USDC_CODE
    txn24.requestAssetIssuer = STELLAR_USDC_ISSUER
    val sep24TxnCapture = slot<JdbcSep24Transaction>()
    val anchorEventCapture = slot<AnchorEvent>()

    mockkStatic(Metrics::class)

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns false
    every { custodyConfig.type } returns NONE
    every { eventSession.publish(capture(anchorEventCapture)) } just Runs
    every { Metrics.counter("sep24.transaction", "status", "pending_user_transfer_start") } returns
      sepTransactionCounter

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { custodyService.createTransaction(ofType(Sep24Transaction::class)) }
    verify(exactly = 1) { sepTransactionCounter.increment() }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.kind = WITHDRAWAL.kind
    expectedSep24Txn.status = PENDING_USR_TRANSFER_START.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.requestAssetCode = STELLAR_USDC_CODE
    expectedSep24Txn.requestAssetIssuer = STELLAR_USDC_ISSUER
    expectedSep24Txn.amountIn = "1"
    expectedSep24Txn.amountInAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "0.9"
    expectedSep24Txn.amountOutAsset = FIAT_USD
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.amountFeeAsset = STELLAR_USDC
    expectedSep24Txn.amountExpected = "1"
    expectedSep24Txn.memo = TEXT_MEMO
    expectedSep24Txn.memoType = TEXT_MEMO_TYPE
    expectedSep24Txn.toAccount = DESTINATION_ACCOUNT
    expectedSep24Txn.withdrawAnchorAccount = DESTINATION_ACCOUNT

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_24
    expectedResponse.kind = WITHDRAWAL
    expectedResponse.status = PENDING_USR_TRANSFER_START
    expectedResponse.amountIn = Amount("1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("0.9", FIAT_USD)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.amountExpected = Amount("1", STELLAR_USDC)
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.memo = TEXT_MEMO
    expectedResponse.memoType = TEXT_MEMO_TYPE
    expectedResponse.destinationAccount = DESTINATION_ACCOUNT

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    val expectedEvent =
      AnchorEvent.builder()
        .id(anchorEventCapture.captured.id)
        .sep(SEP_24.sep.toString())
        .type(TRANSACTION_STATUS_CHANGED)
        .transaction(expectedResponse)
        .build()

    JSONAssert.assertEquals(
      gson.toJson(expectedEvent),
      gson.toJson(anchorEventCapture.captured),
      JSONCompareMode.STRICT
    )

    assertTrue(sep24TxnCapture.captured.updatedAt >= startDate)
    assertTrue(sep24TxnCapture.captured.updatedAt <= endDate)
  }

  @Test
  fun test_handle_ok_autogeneratedMemo() {
    val sep24DepositInfoGenerator: Sep24DepositInfoSelfGenerator = mockk()
    this.handler =
      RequestOnchainFundsHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        custodyService,
        custodyConfig,
        sep24DepositInfoGenerator,
        eventService
      )

    val request =
      RequestOnchainFundsRequest.builder()
        .transactionId(TX_ID)
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("0.9", FIAT_USD))
        .amountFee(AmountAssetRequest("0.1", STELLAR_USDC))
        .amountExpected(AmountRequest("1"))
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.requestAssetCode = STELLAR_USDC_CODE
    txn24.requestAssetIssuer = STELLAR_USDC_ISSUER
    val sep24TxnCapture = slot<JdbcSep24Transaction>()
    val anchorEventCapture = slot<AnchorEvent>()
    val depositInfo = SepDepositInfo(DESTINATION_ACCOUNT_2, TEXT_MEMO_2, TEXT_MEMO_TYPE)

    mockkStatic(Metrics::class)

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns false
    every { custodyConfig.type } returns NONE
    every { sep24DepositInfoGenerator.generate(ofType(Sep24Transaction::class)) } returns
      depositInfo
    every { eventSession.publish(capture(anchorEventCapture)) } just Runs
    every { Metrics.counter("sep24.transaction", "status", "pending_user_transfer_start") } returns
      sepTransactionCounter

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { custodyService.createTransaction(ofType(Sep24Transaction::class)) }
    verify(exactly = 1) { sepTransactionCounter.increment() }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.kind = WITHDRAWAL.kind
    expectedSep24Txn.status = PENDING_USR_TRANSFER_START.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.requestAssetCode = STELLAR_USDC_CODE
    expectedSep24Txn.requestAssetIssuer = STELLAR_USDC_ISSUER
    expectedSep24Txn.amountIn = "1"
    expectedSep24Txn.amountInAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "0.9"
    expectedSep24Txn.amountOutAsset = FIAT_USD
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.amountFeeAsset = STELLAR_USDC
    expectedSep24Txn.amountExpected = "1"
    expectedSep24Txn.memo = TEXT_MEMO_2
    expectedSep24Txn.memoType = TEXT_MEMO_TYPE
    expectedSep24Txn.toAccount = DESTINATION_ACCOUNT_2
    expectedSep24Txn.withdrawAnchorAccount = DESTINATION_ACCOUNT_2

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_24
    expectedResponse.kind = WITHDRAWAL
    expectedResponse.status = PENDING_USR_TRANSFER_START
    expectedResponse.amountIn = Amount("1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("0.9", FIAT_USD)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.amountExpected = Amount("1", STELLAR_USDC)
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.memo = TEXT_MEMO_2
    expectedResponse.memoType = TEXT_MEMO_TYPE
    expectedResponse.destinationAccount = DESTINATION_ACCOUNT_2

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    val expectedEvent =
      AnchorEvent.builder()
        .id(anchorEventCapture.captured.id)
        .sep(SEP_24.sep.toString())
        .type(TRANSACTION_STATUS_CHANGED)
        .transaction(expectedResponse)
        .build()

    JSONAssert.assertEquals(
      gson.toJson(expectedEvent),
      gson.toJson(anchorEventCapture.captured),
      JSONCompareMode.STRICT
    )

    assertTrue(sep24TxnCapture.captured.updatedAt >= startDate)
    assertTrue(sep24TxnCapture.captured.updatedAt <= endDate)
  }

  @Test
  fun test_handle_ok_withExpectedAmount_custodyIntegrationEnabled() {
    val request =
      RequestOnchainFundsRequest.builder()
        .transactionId(TX_ID)
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("0.9", FIAT_USD))
        .amountFee(AmountAssetRequest("0.1", STELLAR_USDC))
        .amountExpected(AmountRequest("1"))
        .memo(TEXT_MEMO)
        .memoType(TEXT_MEMO_TYPE)
        .destinationAccount(DESTINATION_ACCOUNT)
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.requestAssetCode = STELLAR_USDC_CODE
    txn24.requestAssetIssuer = STELLAR_USDC_ISSUER
    val sep24TxnCapture = slot<JdbcSep24Transaction>()
    val sep24CustodyTxnCapture = slot<JdbcSep24Transaction>()
    val anchorEventCapture = slot<AnchorEvent>()

    mockkStatic(Metrics::class)

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns true
    every { custodyConfig.type } returns NONE
    every { custodyService.createTransaction(capture(sep24CustodyTxnCapture)) } just Runs
    every { eventSession.publish(capture(anchorEventCapture)) } just Runs
    every { Metrics.counter("sep24.transaction", "status", "pending_user_transfer_start") } returns
      sepTransactionCounter

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 1) { sepTransactionCounter.increment() }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.kind = WITHDRAWAL.kind
    expectedSep24Txn.status = PENDING_USR_TRANSFER_START.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.requestAssetCode = STELLAR_USDC_CODE
    expectedSep24Txn.requestAssetIssuer = STELLAR_USDC_ISSUER
    expectedSep24Txn.amountIn = "1"
    expectedSep24Txn.amountInAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "0.9"
    expectedSep24Txn.amountOutAsset = FIAT_USD
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.amountFeeAsset = STELLAR_USDC
    expectedSep24Txn.amountExpected = "1"
    expectedSep24Txn.memo = TEXT_MEMO
    expectedSep24Txn.memoType = TEXT_MEMO_TYPE
    expectedSep24Txn.toAccount = DESTINATION_ACCOUNT
    expectedSep24Txn.withdrawAnchorAccount = DESTINATION_ACCOUNT

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(sep24CustodyTxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_24
    expectedResponse.kind = WITHDRAWAL
    expectedResponse.status = PENDING_USR_TRANSFER_START
    expectedResponse.amountIn = Amount("1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("0.9", FIAT_USD)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.amountExpected = Amount("1", STELLAR_USDC)
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.memo = TEXT_MEMO
    expectedResponse.memoType = TEXT_MEMO_TYPE
    expectedResponse.destinationAccount = DESTINATION_ACCOUNT

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    val expectedEvent =
      AnchorEvent.builder()
        .id(anchorEventCapture.captured.id)
        .sep(SEP_24.sep.toString())
        .type(TRANSACTION_STATUS_CHANGED)
        .transaction(expectedResponse)
        .build()

    JSONAssert.assertEquals(
      gson.toJson(expectedEvent),
      gson.toJson(anchorEventCapture.captured),
      JSONCompareMode.STRICT
    )

    assertTrue(sep24TxnCapture.captured.updatedAt >= startDate)
    assertTrue(sep24TxnCapture.captured.updatedAt <= endDate)
  }

  @Test
  fun test_handle_ok_withoutAmountExpected() {
    val request =
      RequestOnchainFundsRequest.builder()
        .transactionId(TX_ID)
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("0.9", FIAT_USD))
        .amountFee(AmountAssetRequest("0.1", STELLAR_USDC))
        .memo(TEXT_MEMO)
        .memoType(TEXT_MEMO_TYPE)
        .destinationAccount(DESTINATION_ACCOUNT)
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.requestAssetCode = STELLAR_USDC_CODE
    txn24.requestAssetIssuer = STELLAR_USDC_ISSUER
    val sep24TxnCapture = slot<JdbcSep24Transaction>()
    val anchorEventCapture = slot<AnchorEvent>()

    mockkStatic(Metrics::class)

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns false
    every { custodyConfig.type } returns NONE
    every { eventSession.publish(capture(anchorEventCapture)) } just Runs
    every { Metrics.counter("sep24.transaction", "status", "pending_user_transfer_start") } returns
      sepTransactionCounter

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { custodyService.createTransaction(ofType(Sep24Transaction::class)) }
    verify(exactly = 1) { sepTransactionCounter.increment() }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.kind = WITHDRAWAL.kind
    expectedSep24Txn.status = PENDING_USR_TRANSFER_START.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.requestAssetCode = STELLAR_USDC_CODE
    expectedSep24Txn.requestAssetIssuer = STELLAR_USDC_ISSUER
    expectedSep24Txn.amountIn = "1"
    expectedSep24Txn.amountInAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "0.9"
    expectedSep24Txn.amountOutAsset = FIAT_USD
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.amountFeeAsset = STELLAR_USDC
    expectedSep24Txn.amountExpected = "1"
    expectedSep24Txn.memo = TEXT_MEMO
    expectedSep24Txn.memoType = TEXT_MEMO_TYPE
    expectedSep24Txn.toAccount = DESTINATION_ACCOUNT
    expectedSep24Txn.withdrawAnchorAccount = DESTINATION_ACCOUNT

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_24
    expectedResponse.kind = WITHDRAWAL
    expectedResponse.status = PENDING_USR_TRANSFER_START
    expectedResponse.amountIn = Amount("1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("0.9", FIAT_USD)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.amountExpected = Amount("1", STELLAR_USDC)
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.memo = TEXT_MEMO
    expectedResponse.memoType = TEXT_MEMO_TYPE
    expectedResponse.destinationAccount = DESTINATION_ACCOUNT

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    val expectedEvent =
      AnchorEvent.builder()
        .id(anchorEventCapture.captured.id)
        .sep(SEP_24.sep.toString())
        .type(TRANSACTION_STATUS_CHANGED)
        .transaction(expectedResponse)
        .build()

    JSONAssert.assertEquals(
      gson.toJson(expectedEvent),
      gson.toJson(anchorEventCapture.captured),
      JSONCompareMode.STRICT
    )

    assertTrue(sep24TxnCapture.captured.updatedAt >= startDate)
    assertTrue(sep24TxnCapture.captured.updatedAt <= endDate)
  }

  @Test
  fun test_handle_ok_withoutAmounts_amountsPresent() {
    val request =
      RequestOnchainFundsRequest.builder()
        .transactionId(TX_ID)
        .memo(TEXT_MEMO)
        .memoType(TEXT_MEMO_TYPE)
        .destinationAccount(DESTINATION_ACCOUNT)
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.requestAssetCode = STELLAR_USDC_CODE
    txn24.requestAssetIssuer = STELLAR_USDC_ISSUER
    txn24.amountIn = "1"
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountOut = "0.9"
    txn24.amountOutAsset = FIAT_USD
    txn24.amountFee = "0.1"
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.amountExpected = "1"
    val sep24TxnCapture = slot<JdbcSep24Transaction>()
    val anchorEventCapture = slot<AnchorEvent>()

    mockkStatic(Metrics::class)

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.isCustodyIntegrationEnabled } returns false
    every { custodyConfig.type } returns NONE
    every { eventSession.publish(capture(anchorEventCapture)) } just Runs
    every { Metrics.counter("sep24.transaction", "status", "pending_user_transfer_start") } returns
      sepTransactionCounter

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { custodyService.createTransaction(ofType(Sep24Transaction::class)) }
    verify(exactly = 1) { sepTransactionCounter.increment() }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.kind = WITHDRAWAL.kind
    expectedSep24Txn.status = PENDING_USR_TRANSFER_START.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.requestAssetCode = STELLAR_USDC_CODE
    expectedSep24Txn.requestAssetIssuer = STELLAR_USDC_ISSUER
    expectedSep24Txn.amountIn = "1"
    expectedSep24Txn.amountInAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "0.9"
    expectedSep24Txn.amountOutAsset = FIAT_USD
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.amountFeeAsset = STELLAR_USDC
    expectedSep24Txn.amountExpected = "1"
    expectedSep24Txn.memo = TEXT_MEMO
    expectedSep24Txn.memoType = TEXT_MEMO_TYPE
    expectedSep24Txn.toAccount = DESTINATION_ACCOUNT
    expectedSep24Txn.withdrawAnchorAccount = DESTINATION_ACCOUNT

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_24
    expectedResponse.kind = WITHDRAWAL
    expectedResponse.status = PENDING_USR_TRANSFER_START
    expectedResponse.amountIn = Amount("1", STELLAR_USDC)
    expectedResponse.amountOut = Amount("0.9", FIAT_USD)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.amountExpected = Amount("1", STELLAR_USDC)
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.memo = TEXT_MEMO
    expectedResponse.memoType = TEXT_MEMO_TYPE
    expectedResponse.destinationAccount = DESTINATION_ACCOUNT

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    val expectedEvent =
      AnchorEvent.builder()
        .id(anchorEventCapture.captured.id)
        .sep(SEP_24.sep.toString())
        .type(TRANSACTION_STATUS_CHANGED)
        .transaction(expectedResponse)
        .build()

    JSONAssert.assertEquals(
      gson.toJson(expectedEvent),
      gson.toJson(anchorEventCapture.captured),
      JSONCompareMode.STRICT
    )

    assertTrue(sep24TxnCapture.captured.updatedAt >= startDate)
    assertTrue(sep24TxnCapture.captured.updatedAt <= endDate)
  }

  @Test
  fun test_handle_withoutAmounts_amountsAbsent() {
    val request = RequestOnchainFundsRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("amount_in is required", ex.message)

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_withoutAmounts_amount_out_absent() {
    val request = RequestOnchainFundsRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.amountIn = "1"
    txn24.amountInAsset = STELLAR_USDC
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("amount_out is required", ex.message)

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_withoutAmounts_amount_fee_absent() {
    val request = RequestOnchainFundsRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.amountIn = "1"
    txn24.amountInAsset = STELLAR_USDC
    txn24.amountOut = "0.9"
    txn24.amountOutAsset = FIAT_USD
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("amount_fee is required", ex.message)

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_notNoneGenerator() {
    val sep24DepositInfoGenerator: Sep24DepositInfoSelfGenerator = mockk()
    this.handler =
      RequestOnchainFundsHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        custodyService,
        custodyConfig,
        sep24DepositInfoGenerator,
        eventService
      )

    val request =
      RequestOnchainFundsRequest.builder()
        .transactionId(TX_ID)
        .memo(TEXT_MEMO)
        .memoType(TEXT_MEMO_TYPE)
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("1", FIAT_USD))
        .amountFee(AmountAssetRequest("1", STELLAR_USDC))
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals(
      "Anchor is not configured to accept memo, memo_type and destination_account",
      ex.message
    )

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_notAllAmounts() {
    val request =
      RequestOnchainFundsRequest.builder()
        .amountIn(AmountAssetRequest("1", FIAT_USD))
        .amountOut(AmountAssetRequest("1", FIAT_USD))
        .transactionId(TX_ID)
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals(
      "All or none of the amount_in, amount_out, and amount_fee should be set",
      ex.message
    )

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_invalidMemo() {
    val request =
      RequestOnchainFundsRequest.builder()
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("1", FIAT_USD))
        .amountFee(AmountAssetRequest("1", STELLAR_USDC))
        .transactionId(TX_ID)
        .memo(TEXT_MEMO)
        .memoType(INVALID_MEMO_TYPE)
        .destinationAccount(DESTINATION_ACCOUNT)
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("Invalid memo or memo_type: Invalid memo type: invalidMemoType", ex.message)

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_notSupportedMemoType() {
    val request =
      RequestOnchainFundsRequest.builder()
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("1", FIAT_USD))
        .amountFee(AmountAssetRequest("1", STELLAR_USDC))
        .transactionId(TX_ID)
        .memo(HASH_MEMO)
        .memoType(HASH_MEMO_TYPE)
        .destinationAccount(DESTINATION_ACCOUNT)
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null
    every { custodyConfig.type } returns FIREBLOCKS

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("Memo type[hash] is not supported for custody type[fireblocks]", ex.message)

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_ok_missingMemo() {
    val request =
      RequestOnchainFundsRequest.builder()
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("1", FIAT_USD))
        .amountFee(AmountAssetRequest("1", STELLAR_USDC))
        .transactionId(TX_ID)
        .destinationAccount(DESTINATION_ACCOUNT)
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("memo and memo_type are required", ex.message)

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_ok_missingDestinationAccount() {
    val request =
      RequestOnchainFundsRequest.builder()
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("1", FIAT_USD))
        .amountFee(AmountAssetRequest("1", STELLAR_USDC))
        .transactionId(TX_ID)
        .memo(TEXT_MEMO)
        .memoType(TEXT_MEMO_TYPE)
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("destination_account is required", ex.message)

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_invalidAmounts() {
    val request =
      RequestOnchainFundsRequest.builder()
        .transactionId(TX_ID)
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("1", FIAT_USD))
        .amountFee(AmountAssetRequest("1", STELLAR_USDC))
        .amountExpected(AmountRequest("1"))
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.requestAssetCode = STELLAR_USDC_CODE
    txn24.requestAssetIssuer = STELLAR_USDC_ISSUER
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    request.amountIn.amount = "-1"
    var ex = assertThrows<BadRequestException> { handler.handle(request) }
    assertEquals("amount_in.amount should be positive", ex.message)
    request.amountIn.amount = "1"

    request.amountOut.amount = "-1"
    ex = assertThrows { handler.handle(request) }
    assertEquals("amount_out.amount should be positive", ex.message)
    request.amountOut.amount = "1"

    request.amountFee.amount = "-1"
    ex = assertThrows { handler.handle(request) }
    assertEquals("amount_fee.amount should be non-negative", ex.message)
    request.amountFee.amount = "1"

    request.amountExpected.amount = "-1"
    ex = assertThrows { handler.handle(request) }
    assertEquals("amount_expected.amount should be positive", ex.message)

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_invalidAssets() {
    val request =
      RequestOnchainFundsRequest.builder()
        .transactionId(TX_ID)
        .amountIn(AmountAssetRequest("1", STELLAR_USDC))
        .amountOut(AmountAssetRequest("1", FIAT_USD))
        .amountFee(AmountAssetRequest("1", STELLAR_USDC))
        .amountExpected(AmountRequest("1"))
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.kind = WITHDRAWAL.kind
    txn24.requestAssetCode = STELLAR_USDC_CODE
    txn24.requestAssetIssuer = STELLAR_USDC_ISSUER
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    request.amountIn.asset = FIAT_USD
    var ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("amount_in.asset should be stellar asset", ex.message)
    request.amountIn.asset = STELLAR_USDC

    request.amountOut.asset = STELLAR_USDC
    ex = assertThrows { handler.handle(request) }
    assertEquals("amount_out.asset should be non-stellar asset", ex.message)
    request.amountOut.asset = FIAT_USD

    request.amountFee.asset = FIAT_USD
    ex = assertThrows { handler.handle(request) }
    assertEquals("amount_fee.asset should be stellar asset", ex.message)

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }
}
