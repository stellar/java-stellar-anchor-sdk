package org.stellar.anchor.platform.action

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.util.*
import javax.validation.Validator
import kotlin.collections.Set
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.AnchorException
import org.stellar.anchor.api.exception.rpc.InvalidParamsException
import org.stellar.anchor.api.exception.rpc.InvalidRequestException
import org.stellar.anchor.api.rpc.action.ActionMethod
import org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_INTERACTIVE_FLOW_COMPLETED
import org.stellar.anchor.api.rpc.action.AmountRequest
import org.stellar.anchor.api.rpc.action.NotifyInteractiveFlowCompletedRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.sep.SepTransactionStatus.*
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.platform.data.JdbcSepTransaction
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
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
    private const val TX_ID = "testId"
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
}
