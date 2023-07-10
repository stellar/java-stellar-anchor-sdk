package org.stellar.anchor.platform.action

import com.google.gson.reflect.TypeToken
import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import java.util.*
import javax.validation.Validator
import kotlin.collections.ArrayList
import kotlin.collections.Set
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
import org.stellar.anchor.api.exception.rpc.InternalErrorException
import org.stellar.anchor.api.exception.rpc.InvalidParamsException
import org.stellar.anchor.api.exception.rpc.InvalidRequestException
import org.stellar.anchor.api.rpc.action.ActionMethod
import org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_INTERACTIVE_FLOW_COMPLETED
import org.stellar.anchor.api.rpc.action.AmountRequest
import org.stellar.anchor.api.rpc.action.NotifyInteractiveFlowCompletedRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.sep.SepTransactionStatus.*
import org.stellar.anchor.api.shared.StellarTransaction
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.platform.data.JdbcSepTransaction
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.util.FileUtil
import org.stellar.anchor.util.GsonUtils
import org.stellar.sdk.Server
import org.stellar.sdk.requests.PaymentsRequestBuilder
import org.stellar.sdk.responses.Page
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import org.stellar.sdk.responses.operations.SetTrustLineFlagsOperationResponse

class ActionHandlerTest {

  // test implementation
  class ActionHandlerTestImpl(
    txn24Store: Sep24TransactionStore,
    txn31Store: Sep31TransactionStore,
    validator: Validator,
    horizon: Horizon,
    assetService: AssetService
  ) :
    ActionHandler<NotifyInteractiveFlowCompletedRequest>(
      txn24Store,
      txn31Store,
      validator,
      horizon,
      assetService,
      NotifyInteractiveFlowCompletedRequest::class.java
    ) {
    override fun getActionType(): ActionMethod {
      return NOTIFY_INTERACTIVE_FLOW_COMPLETED
    }

    override fun getSupportedStatuses(txn: JdbcSepTransaction?): Set<SepTransactionStatus> {
      return setOf(INCOMPLETE, ERROR)
    }

    override fun getSupportedProtocols(): Set<String> {
      return setOf("24", "31")
    }

    override fun updateTransactionWithAction(
      txn: JdbcSepTransaction?,
      request: NotifyInteractiveFlowCompletedRequest?
    ) {}

    override fun getNextStatus(
      txn: JdbcSepTransaction?,
      request: NotifyInteractiveFlowCompletedRequest?
    ): SepTransactionStatus {
      return PENDING_ANCHOR
    }
  }

  companion object {
    private val gson = GsonUtils.getInstance()
    private const val TX_ID = "testId"
    private const val fiatUSD = "iso4217:USD"
  }

  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore

  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore

  @MockK(relaxed = true) private lateinit var validator: Validator

  @MockK(relaxed = true) private lateinit var assetService: AssetService

  @MockK(relaxed = true) private lateinit var horizon: Horizon

  @MockK(relaxed = true) private lateinit var server: Server

  @MockK(relaxed = true) private lateinit var paymentsRequestBuilder: PaymentsRequestBuilder

  @MockK(relaxed = true) private lateinit var page: Page<OperationResponse>

  private lateinit var handler: ActionHandler<NotifyInteractiveFlowCompletedRequest>

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    this.handler = ActionHandlerTestImpl(txn24Store, txn31Store, validator, horizon, assetService)
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
    this.handler = ActionHandlerTestImpl(txn24Store, txn31Store, validator, horizon, assetService)
    val mockAsset = AmountRequest("10", fiatUSD)
    Assertions.assertDoesNotThrow { handler.validateAsset("amount_in", mockAsset) }
    val mockAssetWrongAmount = AmountRequest("10.001", fiatUSD)

    val ex =
      assertThrows<AnchorException> { handler.validateAsset("amount_in", mockAssetWrongAmount) }
    Assertions.assertInstanceOf(InvalidParamsException::class.java, ex)
  }

  @Test
  fun test_isErrorStatus() {
    setOf(ERROR, EXPIRED).forEach { s -> assertTrue(handler.isErrorStatus(s)) }

    Arrays.stream(SepTransactionStatus.values())
      .filter { s -> !setOf(ERROR, EXPIRED).contains(s) }
      .forEach { s -> assertFalse(handler.isErrorStatus(s)) }
  }

  @Test
  fun test_isFinalStatus() {
    setOf(REFUNDED, COMPLETED).forEach { s -> assertTrue(handler.isFinalStatus(s)) }

    Arrays.stream(SepTransactionStatus.values())
      .filter { s -> !setOf(REFUNDED, COMPLETED).contains(s) }
      .forEach { s -> assertFalse(handler.isFinalStatus(s)) }
  }

  @Test
  fun test_addStellarTransaction_paymentType() {
    val txn24 = JdbcSep24Transaction()

    val operationRecordsJson =
      FileUtil.getResourceFileAsString("action/payment_operation_record.json")
    val operationRecordsTypeToken =
      object : TypeToken<ArrayList<PaymentOperationResponse>>() {}.type
    val operationRecords: ArrayList<OperationResponse> =
      gson.fromJson(operationRecordsJson, operationRecordsTypeToken)

    val stellarTransactionsJson =
      FileUtil.getResourceFileAsString("action/payment_stellar_transaction.json")
    val stellarTransactionsToken = object : TypeToken<List<StellarTransaction>>() {}.type
    val stellarTransactions: List<StellarTransaction> =
      gson.fromJson(stellarTransactionsJson, stellarTransactionsToken)

    every { horizon.server } returns server
    every { server.payments() } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.includeTransactions(true) } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.forTransaction("stellarTxId") } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.execute() } returns page
    every { page.records } returns operationRecords

    handler.addStellarTransaction(txn24, "stellarTxId")

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.transferReceivedAt = Instant.parse("2023-05-10T10:18:20Z")
    expectedSep24Txn.stellarTransactionId = "stellarTxId"
    expectedSep24Txn.stellarTransactions = stellarTransactions

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(txn24),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_addStellarTransaction_pathPaymentType() {
    val txn24 = JdbcSep24Transaction()

    val operationRecordsJson =
      FileUtil.getResourceFileAsString("action/path_payment_operation_record.json")
    val operationRecordsTypeToken =
      object : TypeToken<ArrayList<PaymentOperationResponse>>() {}.type
    val operationRecords: ArrayList<OperationResponse> =
      gson.fromJson(operationRecordsJson, operationRecordsTypeToken)

    val stellarTransactionsJson =
      FileUtil.getResourceFileAsString("action/path_payment_stellar_transaction.json")
    val stellarTransactionsToken = object : TypeToken<List<StellarTransaction>>() {}.type
    val stellarTransactions: List<StellarTransaction> =
      gson.fromJson(stellarTransactionsJson, stellarTransactionsToken)

    every { horizon.server } returns server
    every { server.payments() } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.includeTransactions(true) } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.forTransaction("stellarTxId") } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.execute() } returns page
    every { page.records } returns operationRecords

    handler.addStellarTransaction(txn24, "stellarTxId")

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.transferReceivedAt = Instant.parse("2023-05-10T10:18:22Z")
    expectedSep24Txn.stellarTransactionId = "stellarTxId"
    expectedSep24Txn.stellarTransactions = stellarTransactions

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(txn24),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_addStellarTransaction_setTrustType() {
    val txn24 = JdbcSep24Transaction()

    val operationRecordsJson =
      FileUtil.getResourceFileAsString("action/path_payment_operation_record.json")
    val operationRecordsTypeToken =
      object : TypeToken<ArrayList<SetTrustLineFlagsOperationResponse>>() {}.type
    val operationRecords: ArrayList<OperationResponse> =
      gson.fromJson(operationRecordsJson, operationRecordsTypeToken)

    every { horizon.server } returns server
    every { server.payments() } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.includeTransactions(true) } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.forTransaction("stellarTxId") } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.execute() } returns page
    every { page.records } returns operationRecords

    handler.addStellarTransaction(txn24, "stellarTxId")

    val expectedSep24Txn = JdbcSep24Transaction()

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(txn24),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_addStellarTransaction_networkError() {
    val txn24 = JdbcSep24Transaction()

    every { horizon.server } returns server
    every { server.payments() } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.includeTransactions(true) } returns paymentsRequestBuilder
    every { paymentsRequestBuilder.forTransaction("stellarTxId") } throws
      RuntimeException("Invalid stellar transaction")

    val ex =
      assertThrows<InternalErrorException> { handler.addStellarTransaction(txn24, "stellarTxId") }
    assertEquals("Failed to retrieve Stellar transaction by ID[stellarTxId]", ex.message)
  }
}
