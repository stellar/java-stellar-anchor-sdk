package org.stellar.anchor.sep24

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.net.URI
import java.nio.charset.Charset
import org.apache.http.client.utils.URLEncodedUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.Constants
import org.stellar.anchor.Constants.Companion.TEST_ACCOUNT
import org.stellar.anchor.Constants.Companion.TEST_ASSET
import org.stellar.anchor.Constants.Companion.TEST_ASSET_ISSUER_ACCOUNT_ID
import org.stellar.anchor.Constants.Companion.TEST_CLIENT_DOMAIN
import org.stellar.anchor.Constants.Companion.TEST_MEMO
import org.stellar.anchor.Constants.Companion.TEST_TRANSACTION_ID_0
import org.stellar.anchor.Constants.Companion.TEST_TRANSACTION_ID_1
import org.stellar.anchor.TestHelper
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.sep.sep24.GetTransactionRequest
import org.stellar.anchor.api.sep.sep24.GetTransactionsRequest
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.JwtToken
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.Sep24Config
import org.stellar.anchor.util.DateUtil
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.MemoHelper.makeMemo
import org.stellar.sdk.MemoHash
import org.stellar.sdk.MemoId
import org.stellar.sdk.MemoText

internal class Sep24ServiceTest {
  companion object {
    const val TEST_SEP24_INTERACTIVE_URL = "https://test-anchor.stellar.org"
  }

  @MockK(relaxed = true) lateinit var appConfig: AppConfig

  @MockK(relaxed = true) lateinit var sep24Config: Sep24Config

  private val assetService: AssetService = ResourceJsonAssetService("test_assets.json")

  private lateinit var jwtService: JwtService

  @MockK(relaxed = true) private lateinit var txnStore: Sep24TransactionStore

  private lateinit var sep24Service: Sep24Service

  private val gson = GsonUtils.getInstance()

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { appConfig.stellarNetworkPassphrase } returns Constants.TEST_NETWORK_PASS_PHRASE
    every { appConfig.hostUrl } returns Constants.TEST_HOST_URL
    every { appConfig.jwtSecretKey } returns Constants.TEST_JWT_SECRET

    every { sep24Config.interactiveUrl } returns TEST_SEP24_INTERACTIVE_URL
    every { sep24Config.interactiveJwtExpiration } returns 1000

    every { txnStore.newInstance() } returns PojoSep24Transaction()

    jwtService = spyk(JwtService(appConfig))

    sep24Service = Sep24Service(gson, appConfig, sep24Config, assetService, jwtService, txnStore)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  private fun createJwtToken(): JwtToken {
    return TestHelper.createJwtToken(TEST_ACCOUNT, null, appConfig.hostUrl, TEST_CLIENT_DOMAIN)
  }

  private fun createJwtWithMemo(): JwtToken {
    return TestHelper.createJwtToken(TEST_ACCOUNT, TEST_MEMO, appConfig.hostUrl, TEST_CLIENT_DOMAIN)
  }

  @Test
  fun `test withdraw ok`() {
    val slotTxn = slot<Sep24Transaction>()

    every { txnStore.save(capture(slotTxn)) } returns null

    var response =
      sep24Service.withdraw("/sep24/withdraw", createJwtToken(), createTestTransactionRequest())

    verify(exactly = 1) { txnStore.save(any()) }

    assertEquals("interactive_customer_info_needed", response.type)
    assert(response.url.startsWith(TEST_SEP24_INTERACTIVE_URL))
    assertNotNull(response.id)

    assertEquals(slotTxn.captured.status, "incomplete")
    assertEquals(slotTxn.captured.kind, "withdrawal")
    assertEquals(slotTxn.captured.assetCode, "USDC")
    assertEquals(
      slotTxn.captured.assetIssuer,
      "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
    )
    assertEquals(slotTxn.captured.sep10Account, TEST_ACCOUNT)
    assertEquals(slotTxn.captured.fromAccount, TEST_ACCOUNT)
    assertEquals(slotTxn.captured.clientDomain, TEST_CLIENT_DOMAIN)
    assertEquals(slotTxn.captured.amountIn, "123.4")
    assertEquals(slotTxn.captured.amountOut, "123.4")

    var params = URLEncodedUtils.parse(URI(response.url), Charset.forName("UTF-8"))
    var tokenStrings = params.filter { pair -> pair.name.equals("token") }
    assertEquals(tokenStrings.size, 1)
    var tokenString = tokenStrings[0].value
    var decodedToken = jwtService.decode(tokenString)
    assertEquals(decodedToken.sub, TEST_ACCOUNT)
    assertEquals(decodedToken.clientDomain, TEST_CLIENT_DOMAIN)

    // Now test with a memo
    response =
      sep24Service.withdraw("/sep24/withdraw", createJwtWithMemo(), createTestTransactionRequest())

    params = URLEncodedUtils.parse(URI(response.url), Charset.forName("UTF-8"))
    tokenStrings = params.filter { pair -> pair.name.equals("token") }
    assertEquals(tokenStrings.size, 1)
    tokenString = tokenStrings[0].value
    decodedToken = jwtService.decode(tokenString)
    assertEquals(
      "$TEST_ACCOUNT:$TEST_MEMO",
      decodedToken.sub,
    )
    assertEquals(TEST_CLIENT_DOMAIN, decodedToken.clientDomain)
  }

  private fun createTestTransactionRequest(): MutableMap<String, String> {
    return mutableMapOf(
      "lang" to "en",
      "asset_code" to TEST_ASSET,
      "asset_issuer" to TEST_ASSET_ISSUER_ACCOUNT_ID,
      "account" to TEST_ACCOUNT,
      "amount" to "123.4",
      "email_address" to "jamie@stellar.org",
      "first_name" to "Jamie",
      "last_name" to "Li"
    )
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
    var response = sep24Service.deposit("/sep24/deposit", createJwtToken(), request)

    verify(exactly = 1) { txnStore.save(any()) }

    assertEquals("interactive_customer_info_needed", response.type)
    assertTrue(response.url.startsWith(TEST_SEP24_INTERACTIVE_URL))
    assertEquals(response.id, slotTxn.captured.transactionId)

    assertEquals(slotTxn.captured.status, "incomplete")
    assertEquals(slotTxn.captured.kind, "deposit")
    assertEquals(slotTxn.captured.assetCode, TEST_ASSET)
    assertEquals(slotTxn.captured.assetIssuer, TEST_ASSET_ISSUER_ACCOUNT_ID)
    assertEquals(slotTxn.captured.sep10Account, TEST_ACCOUNT)
    assertEquals(slotTxn.captured.toAccount, TEST_ACCOUNT)
    assertEquals(slotTxn.captured.clientDomain, TEST_CLIENT_DOMAIN)
    assertEquals(slotTxn.captured.amountIn, "123.4")
    assertEquals(slotTxn.captured.amountOut, "123.4")

    // Now test with a memo
    response =
      sep24Service.withdraw("/sep24/withdraw", createJwtWithMemo(), createTestTransactionRequest())

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
    assertEquals(response.transactions[0].startedAt, DateUtil.toISO8601UTC(1000))
    assertEquals(response.transactions[0].completedAt, DateUtil.toISO8601UTC(2000))

    assertEquals(response.transactions[1].id, TEST_TRANSACTION_ID_1)
    assertEquals(response.transactions[1].status, "completed")
    assertEquals(response.transactions[1].kind, kind)
    assertEquals(response.transactions[1].startedAt, DateUtil.toISO8601UTC(1000))
    assertEquals(response.transactions[1].completedAt, DateUtil.toISO8601UTC(2000))
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
    assertEquals(response.transaction.startedAt, DateUtil.toISO8601UTC(1000))
    assertEquals(response.transaction.completedAt, DateUtil.toISO8601UTC(2000))
    verify(exactly = 1) { txnStore.findByTransactionId(TEST_TRANSACTION_ID_0) }

    // test with stellar transaction Id
    every { txnStore.findByStellarTransactionId(any()) } returns createTestTransaction("deposit")
    gtr = GetTransactionRequest(null, TEST_TRANSACTION_ID_0, null, "en-US")
    sep24Service.findTransaction(createJwtToken(), gtr)

    verify(exactly = 1) { txnStore.findByStellarTransactionId(TEST_TRANSACTION_ID_0) }

    // test with external transaction Id
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
    var response = sep24Service.withdraw("/sep24/withdraw", createJwtToken(), request)
    assertTrue(response.url.indexOf("lang=en") != -1)
    request.remove("lang")
    response = sep24Service.withdraw("/sep24/withdraw", createJwtToken(), request)
    assertTrue(response.url.indexOf("lang=en-US") != -1)
  }

  private fun createTestTransaction(kind: String): Sep24Transaction {
    val txn = PojoSep24Transaction()
    txn.transactionId = TEST_TRANSACTION_ID_0
    txn.status = "incomplete"
    txn.kind = kind
    txn.startedAt = 1000
    txn.completedAt = 2000

    txn.assetCode = TEST_ASSET
    txn.assetIssuer = TEST_ASSET_ISSUER_ACCOUNT_ID
    txn.sep10Account = TEST_ACCOUNT
    txn.toAccount = TEST_ACCOUNT
    txn.fromAccount = TEST_ACCOUNT
    txn.clientDomain = TEST_CLIENT_DOMAIN
    txn.protocol = "sep24"
    txn.amountIn = "321.4"
    txn.amountOut = "321.4"

    return txn
  }

  private fun createTestTransactions(kind: String): MutableList<Sep24Transaction> {
    val txns = ArrayList<Sep24Transaction>()

    var txn = PojoSep24Transaction()
    txn.transactionId = TEST_TRANSACTION_ID_0
    txn.status = "incomplete"
    txn.kind = kind
    txn.startedAt = 1000
    txn.completedAt = 2000

    txn.assetCode = TEST_ASSET
    txn.assetIssuer = TEST_ASSET_ISSUER_ACCOUNT_ID
    txn.sep10Account = TEST_ACCOUNT
    txn.toAccount = TEST_ACCOUNT
    txn.fromAccount = TEST_ACCOUNT
    txn.clientDomain = TEST_CLIENT_DOMAIN
    txn.protocol = "sep24"
    txn.amountIn = "321.4"
    txn.amountOut = "321.4"
    txns.add(txn)

    txn = PojoSep24Transaction()
    txn.transactionId = TEST_TRANSACTION_ID_1
    txn.status = "completed"
    txn.kind = kind
    txn.startedAt = 1000
    txn.completedAt = 2000

    txn.assetCode = TEST_ASSET
    txn.assetIssuer = TEST_ASSET_ISSUER_ACCOUNT_ID
    txn.sep10Account = TEST_ACCOUNT
    txn.toAccount = TEST_ACCOUNT
    txn.fromAccount = TEST_ACCOUNT
    txn.clientDomain = TEST_CLIENT_DOMAIN
    txn.protocol = "sep24"
    txn.amountIn = "456.7"
    txn.amountOut = "456.7"
    txns.add(txn)

    return txns
  }
}
