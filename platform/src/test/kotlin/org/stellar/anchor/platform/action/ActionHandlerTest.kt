package org.stellar.anchor.platform.action

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import javax.validation.ConstraintViolation
import javax.validation.Validator
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.exception.AnchorException
import org.stellar.anchor.api.exception.rpc.InvalidParamsException
import org.stellar.anchor.api.exception.rpc.InvalidRequestException
import org.stellar.anchor.api.platform.GetTransactionResponse
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind.RECEIVE
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_31
import org.stellar.anchor.api.rpc.action.ActionMethod
import org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_INTERACTIVE_FLOW_COMPLETED
import org.stellar.anchor.api.rpc.action.AmountRequest
import org.stellar.anchor.api.rpc.action.NotifyInteractiveFlowCompletedRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.sep.SepTransactionStatus.*
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.Customers
import org.stellar.anchor.api.shared.StellarId
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.platform.data.JdbcSep31Transaction
import org.stellar.anchor.platform.data.JdbcSepTransaction
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.util.GsonUtils
import org.stellar.sdk.AssetTypeCreditAlphaNum
import org.stellar.sdk.Server
import org.stellar.sdk.requests.AccountsRequestBuilder
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.responses.AccountResponse.Balance
import shadow.com.google.common.base.Optional

class ActionHandlerTest {

  // test implementation
  class ActionHandlerTestImpl(
    txn24Store: Sep24TransactionStore,
    txn31Store: Sep31TransactionStore,
    validator: Validator,
    horizon: Horizon,
    assetService: AssetService,
    private val nextStatus: SepTransactionStatus
  ) :
    ActionHandler<NotifyInteractiveFlowCompletedRequest>(
      txn24Store,
      txn31Store,
      validator,
      horizon,
      assetService
    ) {
    override fun getActionType(): ActionMethod {
      return NOTIFY_INTERACTIVE_FLOW_COMPLETED
    }

    override fun getSupportedStatuses(txn: JdbcSepTransaction?): MutableSet<SepTransactionStatus> {
      return mutableSetOf(INCOMPLETE, ERROR)
    }

    override fun getSupportedProtocols(): MutableSet<String> {
      return mutableSetOf("24", "31")
    }

    override fun updateTransactionWithAction(
      txn: JdbcSepTransaction?,
      request: NotifyInteractiveFlowCompletedRequest?
    ) {
      txn!!.externalTransactionId = EXTERNAL_TX_ID
    }

    override fun getNextStatus(
      txn: JdbcSepTransaction?,
      request: NotifyInteractiveFlowCompletedRequest?
    ): SepTransactionStatus {
      return nextStatus
    }
  }

  companion object {
    private val gson = GsonUtils.getInstance()
    private const val TX_ID = "testId"
    private const val EXTERNAL_TX_ID = "externalTestId"
    private const val TX_MESSAGE_1 = "testMessage1"
    private const val TX_MESSAGE_2 = "testMessage2"
    private const val fiatUSD = "iso4217:USD"
  }

  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore

  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore

  @MockK(relaxed = true) private lateinit var horizon: Horizon

  @MockK(relaxed = true) private lateinit var validator: Validator

  @MockK(relaxed = true) private lateinit var assetService: AssetService

  private lateinit var handler: ActionHandler<NotifyInteractiveFlowCompletedRequest>

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    handler =
      ActionHandlerTestImpl(
        txn24Store,
        txn31Store,
        validator,
        horizon,
        assetService,
        PENDING_ANCHOR
      )
  }

  @Test
  fun test_handle_transactionIsNotFound() {
    val request = NotifyInteractiveFlowCompletedRequest.builder().transactionId(TX_ID).build()
    val tnx24 = JdbcSep24Transaction()
    tnx24.status = INCOMPLETE.toString()

    every { txn24Store.findByTransactionId(any()) } returns null
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals("Transaction with id[testId] is not found", ex.message)
  }

  @Test
  fun test_handle_unsupportedProtocol() {
    val request = NotifyInteractiveFlowCompletedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep31Transaction()
    txn24.status = INCOMPLETE.toString()
    val spyTxn24 = spyk(txn24)

    every { txn24Store.findByTransactionId(any()) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns spyTxn24
    every { spyTxn24.protocol } returns "100"

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Protocol[100] is not supported by action[NOTIFY_INTERACTIVE_FLOW_COMPLETED]",
      ex.message
    )
  }

  @Test
  fun test_handle_unsupportedStatus() {
    val request = NotifyInteractiveFlowCompletedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[NOTIFY_INTERACTIVE_FLOW_COMPLETED] is not supported for status[pending_anchor]",
      ex.message
    )
  }

  @Test
  fun test_handle_invalidRequest() {
    val request = NotifyInteractiveFlowCompletedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()

    val violation1: ConstraintViolation<NotifyInteractiveFlowCompletedRequest> = mockk()
    val violation2: ConstraintViolation<NotifyInteractiveFlowCompletedRequest> = mockk()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { violation1.message } returns "violation error message 1"
    every { violation2.message } returns "violation error message 2"
    every { validator.validate(request) } returns setOf(violation1, violation2)

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("violation error message 1\n" + "violation error message 2", ex.message)
  }

  @Test
  fun test_handle_missingMessage() {
    handler = ActionHandlerTestImpl(txn24Store, txn31Store, validator, horizon, assetService, ERROR)

    val request = NotifyInteractiveFlowCompletedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("message is required", ex.message)
  }

  @Test
  fun test_handle_sep24_ok_withMessage() {
    val request =
      NotifyInteractiveFlowCompletedRequest.builder()
        .transactionId(TX_ID)
        .message(TX_MESSAGE_2)
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = ERROR.toString()
    txn24.kind = DEPOSIT.kind
    txn24.message = TX_MESSAGE_1
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.kind = DEPOSIT.kind
    expectedSep24Txn.status = PENDING_ANCHOR.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.externalTransactionId = EXTERNAL_TX_ID
    expectedSep24Txn.message = TX_MESSAGE_2

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_24
    expectedResponse.kind = DEPOSIT
    expectedResponse.status = PENDING_ANCHOR
    expectedResponse.amountExpected = Amount(null, "")
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.externalTransactionId = EXTERNAL_TX_ID
    expectedResponse.message = TX_MESSAGE_2

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedSep24Txn.updatedAt.isAfter(startDate))
    assertTrue(expectedSep24Txn.updatedAt.isBefore(endDate))
  }

  @Test
  fun test_handle_sep31_ok_withMessage() {
    val request =
      NotifyInteractiveFlowCompletedRequest.builder()
        .transactionId(TX_ID)
        .message(TX_MESSAGE_2)
        .build()
    val txn31 = JdbcSep31Transaction()
    txn31.status = ERROR.toString()
    txn31.requiredInfoMessage = TX_MESSAGE_1
    val txn31Capture = slot<JdbcSep31Transaction>()

    every { txn24Store.findByTransactionId(any()) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns txn31
    every { txn31Store.save(capture(txn31Capture)) } returns null

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn24Store.save(any()) }

    val expectedTxn31 = JdbcSep31Transaction()
    expectedTxn31.status = PENDING_ANCHOR.toString()
    expectedTxn31.updatedAt = txn31Capture.captured.updatedAt
    expectedTxn31.externalTransactionId = EXTERNAL_TX_ID
    expectedTxn31.requiredInfoMessage = TX_MESSAGE_2

    JSONAssert.assertEquals(
      gson.toJson(expectedTxn31),
      gson.toJson(txn31Capture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_31
    expectedResponse.kind = RECEIVE
    expectedResponse.status = PENDING_ANCHOR
    expectedResponse.amountIn = Amount()
    expectedResponse.amountOut = Amount()
    expectedResponse.amountFee = Amount()
    expectedResponse.amountExpected = Amount()
    expectedResponse.customers = Customers(StellarId(), StellarId())
    expectedResponse.updatedAt = txn31Capture.captured.updatedAt
    expectedResponse.externalTransactionId = EXTERNAL_TX_ID
    expectedResponse.message = TX_MESSAGE_2

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedTxn31.updatedAt.isAfter(startDate))
    assertTrue(expectedTxn31.updatedAt.isBefore(endDate))
  }

  @Test
  fun test_handle_sep24_ok_withoutMessage() {
    val request = NotifyInteractiveFlowCompletedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = ERROR.toString()
    txn24.kind = DEPOSIT.kind
    txn24.message = TX_MESSAGE_1
    val txn24Capture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(txn24Capture)) } returns null

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }

    val expectedTxn24 = JdbcSep24Transaction()
    expectedTxn24.kind = DEPOSIT.kind
    expectedTxn24.status = PENDING_ANCHOR.toString()
    expectedTxn24.updatedAt = txn24Capture.captured.updatedAt
    expectedTxn24.externalTransactionId = EXTERNAL_TX_ID

    JSONAssert.assertEquals(
      gson.toJson(expectedTxn24),
      gson.toJson(txn24Capture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_24
    expectedResponse.kind = DEPOSIT
    expectedResponse.status = PENDING_ANCHOR
    expectedResponse.amountExpected = Amount(null, "")
    expectedResponse.updatedAt = txn24Capture.captured.updatedAt
    expectedResponse.externalTransactionId = EXTERNAL_TX_ID

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedTxn24.updatedAt.isAfter(startDate))
    assertTrue(expectedTxn24.updatedAt.isBefore(endDate))
  }

  @Test
  fun test_handle_sep31_ok_withoutMessage() {
    val request = NotifyInteractiveFlowCompletedRequest.builder().transactionId(TX_ID).build()
    val txn31 = JdbcSep31Transaction()
    txn31.status = ERROR.toString()
    txn31.requiredInfoMessage = TX_MESSAGE_1
    val txn31Capture = slot<JdbcSep31Transaction>()

    every { txn24Store.findByTransactionId(any()) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns txn31
    every { txn31Store.save(capture(txn31Capture)) } returns null

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn24Store.save(any()) }

    val expectedTxn31 = JdbcSep31Transaction()
    expectedTxn31.status = PENDING_ANCHOR.toString()
    expectedTxn31.updatedAt = txn31Capture.captured.updatedAt
    expectedTxn31.externalTransactionId = EXTERNAL_TX_ID

    JSONAssert.assertEquals(
      gson.toJson(expectedTxn31),
      gson.toJson(txn31Capture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_31
    expectedResponse.kind = RECEIVE
    expectedResponse.status = PENDING_ANCHOR
    expectedResponse.amountIn = Amount()
    expectedResponse.amountOut = Amount()
    expectedResponse.amountFee = Amount()
    expectedResponse.amountExpected = Amount()
    expectedResponse.customers = Customers(StellarId(), StellarId())
    expectedResponse.updatedAt = txn31Capture.captured.updatedAt
    expectedResponse.externalTransactionId = EXTERNAL_TX_ID

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedTxn31.updatedAt.isAfter(startDate))
    assertTrue(expectedTxn31.updatedAt.isBefore(endDate))
  }

  @Test
  fun test_handle_sep24_ok_finalStatus() {
    handler =
      ActionHandlerTestImpl(txn24Store, txn31Store, validator, horizon, assetService, COMPLETED)

    val request = NotifyInteractiveFlowCompletedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = ERROR.toString()
    txn24.kind = DEPOSIT.kind
    txn24.message = TX_MESSAGE_1
    val txn24Capture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(txn24Capture)) } returns null

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }

    val expectedTxn24 = JdbcSep24Transaction()
    expectedTxn24.kind = DEPOSIT.kind
    expectedTxn24.status = COMPLETED.toString()
    expectedTxn24.updatedAt = txn24Capture.captured.updatedAt
    expectedTxn24.completedAt = txn24Capture.captured.completedAt
    expectedTxn24.externalTransactionId = EXTERNAL_TX_ID

    JSONAssert.assertEquals(
      gson.toJson(expectedTxn24),
      gson.toJson(txn24Capture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_24
    expectedResponse.kind = DEPOSIT
    expectedResponse.status = COMPLETED
    expectedResponse.amountExpected = Amount(null, "")
    expectedResponse.updatedAt = txn24Capture.captured.updatedAt
    expectedResponse.completedAt = txn24Capture.captured.completedAt
    expectedResponse.externalTransactionId = EXTERNAL_TX_ID

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedTxn24.updatedAt.isAfter(startDate))
    assertTrue(expectedTxn24.updatedAt.isBefore(endDate))
    assertTrue(expectedTxn24.completedAt.isAfter(startDate))
    assertTrue(expectedTxn24.completedAt.isBefore(endDate))
  }

  @Test
  fun test_validateAsset_failure() {
    // fails if amount_in.amount is null
    var assetAmount = AmountRequest(null, null)
    var ex = assertThrows<AnchorException> { handler.validateAsset("amount_in", assetAmount) }
    Assertions.assertInstanceOf(InvalidParamsException::class.java, ex)
    Assertions.assertEquals("amount_in.amount cannot be empty", ex.message)

    // fails if amount_in.amount is empty
    assetAmount = AmountRequest("", null)
    ex = assertThrows { handler.validateAsset("amount_in", assetAmount) }
    Assertions.assertInstanceOf(InvalidParamsException::class.java, ex)
    Assertions.assertEquals("amount_in.amount cannot be empty", ex.message)

    // fails if amount_in.amount is invalid
    assetAmount = AmountRequest("abc", null)
    ex = assertThrows { handler.validateAsset("amount_in", assetAmount) }
    Assertions.assertInstanceOf(InvalidParamsException::class.java, ex)
    Assertions.assertEquals("amount_in.amount is invalid", ex.message)

    // fails if amount_in.amount is negative
    assetAmount = AmountRequest("-1", null)
    ex = assertThrows { handler.validateAsset("amount_in", assetAmount) }
    Assertions.assertInstanceOf(InvalidParamsException::class.java, ex)
    Assertions.assertEquals("amount_in.amount should be positive", ex.message)

    // fails if amount_in.amount is zero
    assetAmount = AmountRequest("0", null)
    ex = assertThrows { handler.validateAsset("amount_in", assetAmount) }
    Assertions.assertInstanceOf(InvalidParamsException::class.java, ex)
    Assertions.assertEquals("amount_in.amount should be positive", ex.message)

    // fails if amount_in.asset is empty
    assetAmount = AmountRequest("10", "")
    ex = assertThrows { handler.validateAsset("amount_in", assetAmount) }
    Assertions.assertInstanceOf(InvalidParamsException::class.java, ex)
    Assertions.assertEquals("amount_in.asset cannot be empty", ex.message)

    // fails if listAllAssets is empty
    every { assetService.listAllAssets() } returns listOf()
    val mockAsset = AmountRequest("10", fiatUSD)
    ex = assertThrows { handler.validateAsset("amount_in", mockAsset) }
    Assertions.assertInstanceOf(InvalidParamsException::class.java, ex)
    Assertions.assertEquals("'${fiatUSD}' is not a supported asset.", ex.message)

    // fails if listAllAssets does not contain the desired asset
    ex = assertThrows { handler.validateAsset("amount_in", mockAsset) }
    Assertions.assertInstanceOf(InvalidParamsException::class.java, ex)
    Assertions.assertEquals("'${fiatUSD}' is not a supported asset.", ex.message)
  }

  @Test
  fun test_validateAsset() {
    this.assetService = DefaultAssetService.fromJsonResource("test_assets.json")
    this.handler =
      ActionHandlerTestImpl(
        txn24Store,
        txn31Store,
        validator,
        horizon,
        assetService,
        PENDING_ANCHOR
      )
    val mockAsset = AmountRequest("10", fiatUSD)
    Assertions.assertDoesNotThrow { handler.validateAsset("amount_in", mockAsset) }
    val mockAssetWrongAmount = AmountRequest("10.001", fiatUSD)

    val ex =
      assertThrows<AnchorException> { handler.validateAsset("amount_in", mockAssetWrongAmount) }
    Assertions.assertInstanceOf(InvalidParamsException::class.java, ex)
  }

  @Test
  fun test_isTrustLineConfigured_native() {
    val account = "testAccount"
    val asset = "stellar:native"

    assertTrue(handler.isTrustLineConfigured(account, asset))
  }

  @Test
  fun test_isTrustLineConfigured_horizonError() {
    val account = "testAccount"
    val asset = "stellar:USDC:issuerAccount"

    every { horizon.server } throws RuntimeException("Horizon error")

    assertFalse(handler.isTrustLineConfigured(account, asset))
  }

  @Test
  fun test_isTrustLineConfigured_present() {
    val account = "testAccount"
    val asset = "stellar:USDC:issuerAccount1"
    val server: Server = mockk()
    val accountsRequestBuilder: AccountsRequestBuilder = mockk()
    val accountResponse: AccountResponse = mockk()

    val balance1: Balance = mockk()
    val balance2: Balance = mockk()

    val asset1: AssetTypeCreditAlphaNum = mockk()
    val asset2: AssetTypeCreditAlphaNum = mockk()

    every { horizon.server } returns server
    every { server.accounts() } returns accountsRequestBuilder
    every { accountsRequestBuilder.account(account) } returns accountResponse
    every { balance1.getAssetType() } returns "credit_alphanum4"
    every { balance1.getAsset() } returns Optional.of(asset1)
    every { balance2.getAssetType() } returns "credit_alphanum12"
    every { balance2.getAsset() } returns Optional.of(asset2)
    every { asset1.getCode() } returns "USDC"
    every { asset1.getIssuer() } returns "issuerAccount1"
    every { asset2.getCode() } returns "USDC"
    every { asset2.getIssuer() } returns "issuerAccount2"
    every { accountResponse.getBalances() } returns arrayOf(balance1, balance2)

    assertTrue(handler.isTrustLineConfigured(account, asset))
  }

  @Test
  fun test_isTrustLineConfigured_absent() {
    val account = "testAccount"
    val asset = "stellar:USDC:issuerAccount1"
    val server: Server = mockk()
    val accountsRequestBuilder: AccountsRequestBuilder = mockk()
    val accountResponse: AccountResponse = mockk()

    val balance1: Balance = mockk()
    val balance2: Balance = mockk()
    val balance3: Balance = mockk()

    val asset1: AssetTypeCreditAlphaNum = mockk()
    val asset2: AssetTypeCreditAlphaNum = mockk()
    val asset3: AssetTypeCreditAlphaNum = mockk()

    every { horizon.server } returns server
    every { server.accounts() } returns accountsRequestBuilder
    every { accountsRequestBuilder.account(account) } returns accountResponse
    every { balance1.getAssetType() } returns "credit_alphanum8"
    every { balance1.getAsset() } returns Optional.of(asset1)
    every { balance2.getAssetType() } returns "credit_alphanum4"
    every { balance2.getAsset() } returns Optional.of(asset2)
    every { balance3.getAssetType() } returns "credit_alphanum4"
    every { balance3.getAsset() } returns Optional.of(asset3)
    every { asset1.getCode() } returns "USDC"
    every { asset1.getIssuer() } returns "issuerAccount1"
    every { asset2.getCode() } returns "SRT"
    every { asset2.getIssuer() } returns "issuerAccount1"
    every { asset3.getCode() } returns "USDC"
    every { asset3.getIssuer() } returns "issuerAccount2"
    every { accountResponse.getBalances() } returns arrayOf(balance1, balance2, balance3)

    assertFalse(handler.isTrustLineConfigured(account, asset))
  }
}
