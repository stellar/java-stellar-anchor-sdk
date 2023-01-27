@file:Suppress("unused")

package org.stellar.anchor.sep24

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.net.URI
import java.nio.charset.Charset
import java.time.Instant
import org.apache.http.client.utils.URLEncodedUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.TestConstants
import org.stellar.anchor.TestConstants.Companion.TEST_ACCOUNT
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET_ISSUER_ACCOUNT_ID
import org.stellar.anchor.TestConstants.Companion.TEST_CLIENT_DOMAIN
import org.stellar.anchor.TestConstants.Companion.TEST_MEMO
import org.stellar.anchor.TestConstants.Companion.TEST_TRANSACTION_ID_0
import org.stellar.anchor.TestConstants.Companion.TEST_TRANSACTION_ID_1
import org.stellar.anchor.TestHelper
import org.stellar.anchor.api.callback.FeeIntegration
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.sep.sep24.GetTransactionRequest
import org.stellar.anchor.api.sep.sep24.GetTransactionsRequest
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.JwtToken
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.SecretConfig
import org.stellar.anchor.config.Sep24Config
import org.stellar.anchor.event.EventService
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.MemoHelper.makeMemo
import org.stellar.sdk.MemoHash
import org.stellar.sdk.MemoId
import org.stellar.sdk.MemoText

internal class Sep24ServiceTest {
  companion object {
    const val TEST_SEP24_INTERACTIVE_URL = "https://test-anchor.stellar.org"
    const val TEST_SEP24_MORE_INFO_URL = "https://test-anchor.stellar.org/more_info_url"
    val TEST_STARTED_AT: Instant = Instant.now()
    val TEST_COMPLETED_AT: Instant = Instant.now().plusSeconds(100)
  }

  @MockK(relaxed = true) lateinit var appConfig: AppConfig
  @MockK(relaxed = true) lateinit var secretConfig: SecretConfig
  @MockK(relaxed = true) lateinit var sep24Config: Sep24Config
  @MockK(relaxed = true) lateinit var eventService: EventService
  @MockK(relaxed = true) lateinit var feeIntegration: FeeIntegration
  @MockK(relaxed = true) lateinit var txnStore: Sep24TransactionStore
  @MockK(relaxed = true) lateinit var interactiveUrlConstructor: InteractiveUrlConstructor
  @MockK(relaxed = true) lateinit var moreInfoUrlConstructor: MoreInfoUrlConstructor

  private val assetService: AssetService = DefaultAssetService.fromResource("test_assets.json")

  private lateinit var jwtService: JwtService
  private lateinit var sep24Service: Sep24Service
  private lateinit var createdJwt: JwtToken

  private val gson = GsonUtils.getInstance()

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { appConfig.stellarNetworkPassphrase } returns TestConstants.TEST_NETWORK_PASS_PHRASE
    every { appConfig.hostUrl } returns TestConstants.TEST_HOST_URL
    every { secretConfig.sep10JwtSecretKey } returns TestConstants.TEST_JWT_SECRET
    every { sep24Config.interactiveJwtExpiration } returns 1000
    every { txnStore.newInstance() } returns PojoSep24Transaction()

    jwtService = spyk(JwtService(secretConfig))
    createdJwt = createJwtToken()
    val strToken = jwtService.encode(createdJwt)
    every { interactiveUrlConstructor.construct(any(), any(), any(), any()) } returns
      "${TEST_SEP24_INTERACTIVE_URL}?lang=en&token=$strToken"
    every { moreInfoUrlConstructor.construct(any()) } returns
      "${TEST_SEP24_MORE_INFO_URL}?lang=en&token=$strToken"

    sep24Service =
      Sep24Service(
        appConfig,
        sep24Config,
        assetService,
        jwtService,
        txnStore,
        eventService,
        interactiveUrlConstructor,
        moreInfoUrlConstructor
      )
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `test withdraw ok`() {
    val slotTxn = slot<Sep24Transaction>()

    every { txnStore.save(capture(slotTxn)) } returns null

    val response =
      sep24Service.withdraw("/sep24/withdraw", createdJwt, createTestTransactionRequest())

    verify(exactly = 1) { txnStore.save(any()) }

    assertEquals("interactive_customer_info_needed", response.type)
    assert(response.url.startsWith(TEST_SEP24_INTERACTIVE_URL))
    assertNotNull(response.id)

    assertEquals(slotTxn.captured.status, "incomplete")
    assertEquals(slotTxn.captured.kind, "withdrawal")
    assertEquals(slotTxn.captured.requestAssetCode, "USDC")
    assertEquals(
      slotTxn.captured.requestAssetIssuer,
      "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
    )
    assertEquals(slotTxn.captured.sep10Account, TEST_ACCOUNT)
    assertEquals(slotTxn.captured.fromAccount, TEST_ACCOUNT)
    assertEquals(slotTxn.captured.clientDomain, TEST_CLIENT_DOMAIN)
    assertEquals(slotTxn.captured.requestedAmount, "123.4")

    val params = URLEncodedUtils.parse(URI(response.url), Charset.forName("UTF-8"))
    val tokenStrings = params.filter { pair -> pair.name.equals("token") }
    assertEquals(tokenStrings.size, 1)
    val tokenString = tokenStrings[0].value
    val decodedToken = jwtService.decode(tokenString)
    assertEquals(decodedToken.sub, TEST_ACCOUNT)
    assertEquals(decodedToken.clientDomain, TEST_CLIENT_DOMAIN)
  }

  @Test
  fun `test withdraw with token memo`() {
    createdJwt = createJwtWithMemo()
    val strToken = jwtService.encode(createdJwt)
    every { interactiveUrlConstructor.construct(any(), any(), any(), any()) } returns
      "${TEST_SEP24_INTERACTIVE_URL}?lang=en&token=$strToken"

    val response =
      sep24Service.withdraw("/sep24/withdraw", createdJwt, createTestTransactionRequest())

    val params = URLEncodedUtils.parse(URI(response.url), Charset.forName("UTF-8"))
    val tokenStrings = params.filter { pair -> pair.name.equals("token") }
    assertEquals(tokenStrings.size, 1)
    val tokenString = tokenStrings[0].value
    val decodedToken = jwtService.decode(tokenString)
    assertEquals(
      "$TEST_ACCOUNT:$TEST_MEMO",
      decodedToken.sub,
    )
    assertEquals(TEST_CLIENT_DOMAIN, decodedToken.clientDomain)
  }

  @Test
  fun `test withdrawal with no token and no request failure`() {
    assertThrows<SepValidationException> {
      sep24Service.withdraw("/sep24/withdrawal", null, createTestTransactionRequest())
    }

    assertThrows<SepValidationException> {
      sep24Service.withdraw("/sep24/withdrawal", createJwtToken(), null)
    }
  }

  @Test
  fun `test withdraw with bad requests`() {
    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request.remove("asset_code")
      sep24Service.withdraw("/sep24/withdrawal", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request.remove("account")
      sep24Service.withdraw("/sep24/withdrawal", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request["account"] = "G1234"
      sep24Service.withdraw("/sep24/withdrawal", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request["asset_code"] = "USDC_NA"
      sep24Service.withdraw("/sep24/withdrawal", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request["amount"] = "0"
      sep24Service.withdraw("/sep24/withdrawal", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request["amount"] = "10001"
      sep24Service.withdraw("/sep24/withdrawal", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request["account"] = "G1234"
      val token = createJwtToken()
      token.sub = "G1234"
      sep24Service.withdraw("/sep24/withdrawal", token, request)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["true", "false"])
  fun `test deposit`(claimable_balance_supported: String) {
    val slotTxn = slot<Sep24Transaction>()

    every { txnStore.save(capture(slotTxn)) } returns null

    val request = createTestTransactionRequest()
    request["claimable_balance_supported"] = claimable_balance_supported
    val response = sep24Service.deposit("/sep24/deposit", createdJwt, request)

    verify(exactly = 1) { txnStore.save(any()) }

    assertEquals("interactive_customer_info_needed", response.type)
    assertTrue(response.url.startsWith(TEST_SEP24_INTERACTIVE_URL))
    assertEquals(response.id, slotTxn.captured.transactionId)

    assertEquals(slotTxn.captured.status, "incomplete")
    assertEquals(slotTxn.captured.kind, "deposit")
    assertEquals(slotTxn.captured.requestAssetCode, TEST_ASSET)
    assertEquals(slotTxn.captured.requestAssetIssuer, TEST_ASSET_ISSUER_ACCOUNT_ID)
    assertEquals(slotTxn.captured.sep10Account, TEST_ACCOUNT)
    assertEquals(slotTxn.captured.toAccount, TEST_ACCOUNT)
    assertEquals(slotTxn.captured.clientDomain, TEST_CLIENT_DOMAIN)
    assertEquals(slotTxn.captured.requestedAmount, "123.4")
  }

  @Test
  fun `test deposit with token memo`() {
    createdJwt = createJwtWithMemo()
    val strToken = jwtService.encode(createdJwt)
    every { interactiveUrlConstructor.construct(any(), any(), any(), any()) } returns
      "${TEST_SEP24_INTERACTIVE_URL}?lang=en&token=$strToken"

    val response =
      sep24Service.withdraw("/sep24/withdraw", createdJwt, createTestTransactionRequest())

    val params = URLEncodedUtils.parse(URI(response.url), Charset.forName("UTF-8"))
    val tokenStrings = params.filter { pair -> pair.name.equals("token") }
    assertEquals(tokenStrings.size, 1)
    val tokenString = tokenStrings[0].value
    val decodedToken = jwtService.decode(tokenString)
    assertEquals(
      "$TEST_ACCOUNT:$TEST_MEMO",
      decodedToken.sub,
    )
    assertEquals(TEST_CLIENT_DOMAIN, decodedToken.clientDomain)
  }

  @Test
  fun `test deposit with no token and no request`() {
    assertThrows<SepValidationException> {
      sep24Service.deposit("/sep24/deposit", null, createTestTransactionRequest())
    }

    assertThrows<SepValidationException> {
      sep24Service.deposit("/sep24/deposit", createJwtToken(), null)
    }
  }

  @Test
  fun `test deposit with bad requests`() {
    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request.remove("asset_code")
      sep24Service.deposit("/sep24/deposit", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request.remove("account")
      sep24Service.deposit("/sep24/deposit", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request["account"] = "G1234"
      sep24Service.deposit("/sep24/deposit", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request["asset_code"] = "USDC_NA"
      sep24Service.deposit("/sep24/deposit", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request["amount"] = "0"
      sep24Service.deposit("/sep24/deposit", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request["amount"] = "10001"
      sep24Service.deposit("/sep24/deposit", createJwtToken(), request)
    }

    assertThrows<SepValidationException> {
      val request = createTestTransactionRequest()
      request["account"] = "G1234"
      val token = createJwtToken()
      token.sub = "G1234"
      sep24Service.deposit("/sep24/deposit", token, request)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["deposit", "withdrawal"])
  fun `test find transactions`(kind: String) {
    every { txnStore.findTransactions(TEST_ACCOUNT, any(), any()) } returns
      createTestTransactions(kind)
    val gtr =
      GetTransactionsRequest.of(TEST_ASSET, kind, 10, "2021-12-20T19:30:58+00:00", "1", "en-US")
    val response = sep24Service.findTransactions(createJwtToken(), gtr)

    assertEquals(response.transactions.size, 2)
    assertEquals(response.transactions[0].id, TEST_TRANSACTION_ID_0)
    assertEquals(response.transactions[0].status, "incomplete")
    assertEquals(response.transactions[0].kind, kind)
    assertEquals(response.transactions[0].startedAt, TEST_STARTED_AT)
    assertEquals(response.transactions[0].completedAt, TEST_COMPLETED_AT)

    assertEquals(response.transactions[1].id, TEST_TRANSACTION_ID_1)
    assertEquals(response.transactions[1].status, "completed")
    assertEquals(response.transactions[1].kind, kind)
    assertEquals(response.transactions[1].startedAt, TEST_STARTED_AT)
    assertEquals(response.transactions[1].completedAt, TEST_COMPLETED_AT)
  }

  @ParameterizedTest
  @ValueSource(strings = ["deposit", "withdrawal"])
  fun `test find transactions with validation error`(kind: String) {
    assertThrows<SepNotAuthorizedException> {
      val gtr =
        GetTransactionsRequest.of(TEST_ASSET, kind, 10, "2021-12-20T19:30:58+00:00", "1", "en-US")
      sep24Service.findTransactions(null, gtr)
    }

    assertThrows<SepValidationException> {
      val gtr =
        GetTransactionsRequest.of(
          "BAD_ASSET_CODE",
          kind,
          10,
          "2021-12-20T19:30:58+00:00",
          "1",
          "en-US"
        )
      sep24Service.findTransactions(createJwtToken(), gtr)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["deposit", "withdrawal"])
  fun `test find one transaction`(kind: String) {
    every { txnStore.findByTransactionId(any()) } returns createTestTransaction(kind)

    var gtr = GetTransactionRequest(TEST_TRANSACTION_ID_0, null, null, "en-US")
    val response = sep24Service.findTransaction(createJwtToken(), gtr)

    assertEquals(response.transaction.id, TEST_TRANSACTION_ID_0)
    assertEquals(response.transaction.status, "incomplete")
    assertEquals(response.transaction.kind, kind)
    assertEquals(response.transaction.startedAt, TEST_STARTED_AT)
    assertEquals(response.transaction.completedAt, TEST_COMPLETED_AT)
    verify(exactly = 1) { txnStore.findByTransactionId(TEST_TRANSACTION_ID_0) }

    // test with stellar transaction_id
    every { txnStore.findByStellarTransactionId(any()) } returns createTestTransaction("deposit")
    gtr = GetTransactionRequest(null, TEST_TRANSACTION_ID_0, null, "en-US")
    sep24Service.findTransaction(createJwtToken(), gtr)

    verify(exactly = 1) { txnStore.findByStellarTransactionId(TEST_TRANSACTION_ID_0) }

    // test with external transaction_id
    every { txnStore.findByExternalTransactionId(any()) } returns createTestTransaction("deposit")
    gtr = GetTransactionRequest(null, null, TEST_TRANSACTION_ID_0, "en-US")
    sep24Service.findTransaction(createJwtToken(), gtr)

    verify(exactly = 1) { txnStore.findByExternalTransactionId(TEST_TRANSACTION_ID_0) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["deposit", "withdrawal"])
  fun `test find transaction validation error`(kind: String) {
    assertThrows<SepNotAuthorizedException> {
      val gtr = GetTransactionRequest(TEST_TRANSACTION_ID_0, null, null, "en-US")
      sep24Service.findTransaction(null, gtr)
    }

    assertThrows<SepValidationException> { sep24Service.findTransaction(createJwtToken(), null) }

    assertThrows<SepValidationException> {
      val gtr = GetTransactionRequest(null, null, null, "en-US")
      sep24Service.findTransaction(createJwtToken(), gtr)
    }

    every { txnStore.findByTransactionId(any()) } returns null
    assertThrows<SepNotFoundException> {
      val gtr = GetTransactionRequest(TEST_TRANSACTION_ID_0, null, null, "en-US")
      sep24Service.findTransaction(createJwtToken(), gtr)
    }

    val badTxn = createTestTransaction(kind)
    badTxn.kind = "na"
    every { txnStore.findByTransactionId(any()) } returns badTxn

    assertThrows<SepException> {
      val gtr = GetTransactionRequest(TEST_TRANSACTION_ID_0, null, null, "en-US")
      sep24Service.findTransaction(createJwtToken(), gtr)
    }
  }

  @Test
  fun `test GET info`() {
    val response = sep24Service.info

    assertEquals(3, response.deposit.size)
    assertEquals(1, response.withdraw.size)
    assertNotNull(response.deposit["USDC"])
    assertNotNull(response.withdraw["USDC"])
    assertTrue(response.fee.enabled)
  }

  @Test
  fun `test make memo`() {
    var memo = makeMemo("this_is_a_test_memo", "text")
    assertTrue(memo is MemoText)
    memo = makeMemo("1234", "id")
    assertTrue(memo is MemoId)
    memo = makeMemo("YzVlMzg5ZDMtZGQ4My00NDlmLThhMDctYTUwM2MwM2U=", "hash")
    assertTrue(memo is MemoHash)
  }

  @Test
  fun `test lang`() {
    val slotTxn = slot<Sep24Transaction>()

    every { txnStore.save(capture(slotTxn)) } returns null

    val request = createTestTransactionRequest()
    request["lang"] = "en"
    val response = sep24Service.withdraw("/sep24/withdraw", createJwtToken(), request)
    assertTrue(response.url.indexOf("lang=en") != -1)
  }

  private fun createJwtToken(): JwtToken {
    return TestHelper.createJwtToken(TEST_ACCOUNT, null, appConfig.hostUrl, TEST_CLIENT_DOMAIN)
  }

  private fun createJwtWithMemo(): JwtToken {
    return TestHelper.createJwtToken(TEST_ACCOUNT, TEST_MEMO, appConfig.hostUrl, TEST_CLIENT_DOMAIN)
  }
}
