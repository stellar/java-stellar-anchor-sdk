package org.stellar.anchor.sep10

import com.google.common.io.BaseEncoding
import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.Constants.Companion.TEST_ACCOUNT
import org.stellar.anchor.Constants.Companion.TEST_CLIENT_DOMAIN
import org.stellar.anchor.Constants.Companion.TEST_CLIENT_TOML
import org.stellar.anchor.Constants.Companion.TEST_HOME_DOMAIN
import org.stellar.anchor.Constants.Companion.TEST_HOST_URL
import org.stellar.anchor.Constants.Companion.TEST_JWT_SECRET
import org.stellar.anchor.Constants.Companion.TEST_MEMO
import org.stellar.anchor.Constants.Companion.TEST_NETWORK_PASS_PHRASE
import org.stellar.anchor.Constants.Companion.TEST_SIGNING_SEED
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.sep.sep10.ChallengeRequest
import org.stellar.anchor.api.sep.sep10.ValidationRequest
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.Sep10Config
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.util.FileUtil
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.NetUtil
import org.stellar.sdk.*
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.responses.AccountResponse
import shadow.com.google.gson.annotations.SerializedName

@Suppress("unused")
internal class TestSigner(
  @SerializedName("key") val key: String,
  @SerializedName("type") val type: String,
  @SerializedName("weight") val weight: Int,
  @SerializedName("sponsor") val sponsor: String
) {
  fun toSigner(): AccountResponse.Signer {
    val gson = GsonUtils.getInstance()
    val json = gson.toJson(this)
    return gson.fromJson(json, AccountResponse.Signer::class.java)
  }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
internal class Sep10ServiceTest {
  companion object {
    @JvmStatic
    fun homeDomains(): Stream<String> {
      return Stream.of(null, TEST_HOME_DOMAIN)
    }

    val testAccountWithNoncompliantSigner: String =
      FileUtil.getResourceFileAsString("test_account_with_noncompliant_signer.json")
  }

  @MockK(relaxed = true) private lateinit var appConfig: AppConfig
  @MockK(relaxed = true) private lateinit var sep10Config: Sep10Config
  @MockK(relaxed = true) private lateinit var horizon: Horizon

  private lateinit var jwtService: JwtService
  private lateinit var sep10Service: Sep10Service
  private val clientKeyPair = KeyPair.random()
  private val clientDomainKeyPair = KeyPair.random()

  private lateinit var httpClient: OkHttpClient

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { sep10Config.signingSeed } returns TEST_SIGNING_SEED
    every { sep10Config.homeDomain } returns TEST_HOME_DOMAIN
    every { sep10Config.clientAttributionDenyList } returns listOf("")
    every { sep10Config.clientAttributionAllowList } returns listOf(TEST_CLIENT_DOMAIN)
    every { sep10Config.authTimeout } returns 900
    every { sep10Config.jwtTimeout } returns 900

    every { appConfig.stellarNetworkPassphrase } returns TEST_NETWORK_PASS_PHRASE
    every { appConfig.hostUrl } returns TEST_HOST_URL
    every { appConfig.jwtSecretKey } returns TEST_JWT_SECRET

    mockkStatic(NetUtil::class)
    mockkStatic(Sep10Challenge::class)

    every { NetUtil.fetch(any()) } returns TEST_CLIENT_TOML

    this.jwtService = spyk(JwtService(appConfig))
    this.sep10Service = Sep10Service(appConfig, sep10Config, horizon, jwtService)

    this.httpClient = `create httpClient that trust all certificates`()
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  fun `create httpClient that trust all certificates`(): OkHttpClient {
    val trustAllCerts =
      arrayOf<TrustManager>(
        object : X509TrustManager {
          override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

          override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

          override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        }
      )

    // Install the all-trusting trust manager
    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCerts, SecureRandom())
    return OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.MINUTES)
      .readTimeout(10, TimeUnit.MINUTES)
      .writeTimeout(10, TimeUnit.MINUTES)
      .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
      .hostnameVerifier { _, _ -> true }
      .build()
  }

  @Test
  fun `test the challenge with existent account, multisig, and client domain`() {
    // 1 ------ Create Test Transaction

    // serverKP does not exist in the network.
    val serverWebAuthDomain = TEST_HOME_DOMAIN
    val serverKP = KeyPair.random()

    // clientDomainKP doesn't exist in the network. Refers to the walletAcc (like Lobstr's)
    val clientDomainKP = KeyPair.random()

    // Master account of the multisig. It'll be created in the network.
    val clientMasterKP = KeyPair.random()
    val clientAddress = clientMasterKP.accountId
    // Secondary account of the multisig. It'll be created in the network.
    val clientSecondaryKP = KeyPair.random()

    val nonce = ByteArray(48)
    val random = SecureRandom()
    random.nextBytes(nonce)
    val base64Encoding = BaseEncoding.base64()
    val encodedNonce = base64Encoding.encode(nonce).toByteArray()

    val sourceAccount = Account(serverKP.accountId, -1L)
    val op1DomainNameMandatory =
      ManageDataOperation.Builder("$serverWebAuthDomain auth", encodedNonce)
        .setSourceAccount(clientAddress)
        .build()
    val op2WebAuthDomainMandatory =
      ManageDataOperation.Builder("web_auth_domain", serverWebAuthDomain.toByteArray())
        .setSourceAccount(serverKP.accountId)
        .build()
    val op3clientDomainOptional =
      ManageDataOperation.Builder("client_domain", "lobstr.co".toByteArray())
        .setSourceAccount(clientDomainKP.accountId)
        .build()

    val transaction =
      TransactionBuilder(AccountConverter.enableMuxed(), sourceAccount, Network.TESTNET)
        .addTimeBounds(TimeBounds.expiresAfter(900))
        .setBaseFee(100)
        .addOperation(op1DomainNameMandatory)
        .addOperation(op2WebAuthDomainMandatory)
        .addOperation(op3clientDomainOptional)
        .build()

    transaction.sign(serverKP)
    transaction.sign(clientDomainKP)
    transaction.sign(clientMasterKP)
    transaction.sign(clientSecondaryKP)

    // 2 ------ Create Services
    every { sep10Config.signingSeed } returns String(serverKP.secretSeed)
    every { appConfig.horizonUrl } returns "https://horizon-testnet.stellar.org"
    every { appConfig.stellarNetworkPassphrase } returns TEST_NETWORK_PASS_PHRASE
    val horizon = Horizon(appConfig)
    this.sep10Service = Sep10Service(appConfig, sep10Config, horizon, jwtService)

    // 3 ------ Setup multisig
    val httpRequest =
      Request.Builder()
        .url("https://horizon-testnet.stellar.org/friendbot?addr=" + clientMasterKP.accountId)
        .header("Content-Type", "application/json")
        .get()
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertEquals(200, response.code)

    val clientAccount = horizon.server.accounts().account(clientMasterKP.accountId)
    val multisigTx =
      TransactionBuilder(AccountConverter.enableMuxed(), clientAccount, Network.TESTNET)
        .addTimeBounds(TimeBounds.expiresAfter(900))
        .setBaseFee(300)
        .addOperation(
          SetOptionsOperation.Builder()
            .setLowThreshold(20)
            .setMediumThreshold(20)
            .setHighThreshold(20)
            .setSigner(Signer.ed25519PublicKey(clientSecondaryKP), 10)
            .setMasterKeyWeight(10)
            .build()
        )
        .build()
    multisigTx.sign(clientMasterKP)
    horizon.server.submitTransaction(multisigTx)

    // 4 ------ Run tests
    val validationRequest = ValidationRequest.of(transaction.toEnvelopeXdrBase64())
    assertDoesNotThrow { sep10Service.validateChallenge(validationRequest) }
  }

  @Test
  fun `test challenge with non existent account and client domain`() {
    // 1 ------ Create Test Transaction

    // serverKP does not exist in the network.
    val serverWebAuthDomain = TEST_HOME_DOMAIN
    val serverKP = KeyPair.random()

    // clientDomainKP does not exist in the network. It refers to the wallet (like Lobstr's)
    // account.
    val clientDomainKP = KeyPair.random()

    // The public key of the client that DOES NOT EXIST.
    val clientKP = KeyPair.random()

    val nonce = ByteArray(48)
    val random = SecureRandom()
    random.nextBytes(nonce)
    val base64Encoding = BaseEncoding.base64()
    val encodedNonce = base64Encoding.encode(nonce).toByteArray()

    val sourceAccount = Account(serverKP.accountId, -1L)
    val op1DomainNameMandatory =
      ManageDataOperation.Builder("$serverWebAuthDomain auth", encodedNonce)
        .setSourceAccount(clientKP.accountId)
        .build()
    val op2WebAuthDomainMandatory =
      ManageDataOperation.Builder("web_auth_domain", serverWebAuthDomain.toByteArray())
        .setSourceAccount(serverKP.accountId)
        .build()
    val op3clientDomainOptional =
      ManageDataOperation.Builder("client_domain", "lobstr.co".toByteArray())
        .setSourceAccount(clientDomainKP.accountId)
        .build()

    val transaction =
      TransactionBuilder(AccountConverter.enableMuxed(), sourceAccount, Network.TESTNET)
        .addTimeBounds(TimeBounds.expiresAfter(900))
        .setBaseFee(100)
        .addOperation(op1DomainNameMandatory)
        .addOperation(op2WebAuthDomainMandatory)
        .addOperation(op3clientDomainOptional)
        .build()

    transaction.sign(serverKP)
    transaction.sign(clientDomainKP)
    transaction.sign(clientKP)

    // 2 ------ Create Services
    every { sep10Config.signingSeed } returns String(serverKP.secretSeed)
    every { appConfig.horizonUrl } returns "https://horizon-testnet.stellar.org"
    every { appConfig.stellarNetworkPassphrase } returns TEST_NETWORK_PASS_PHRASE
    val horizon = Horizon(appConfig)
    this.sep10Service = Sep10Service(appConfig, sep10Config, horizon, jwtService)

    // 3 ------ Run tests
    val validationRequest = ValidationRequest.of(transaction.toEnvelopeXdrBase64())
    assertDoesNotThrow { sep10Service.validateChallenge(validationRequest) }
  }

  @Test
  fun `test challenge with existent account multisig with invalid ed dsa public key and client domain`() {
    // 1 ------ Mock client account and its response from horizon
    // The public key of the client that exists thanks to a mockk
    // GDFWZYGUNUFW4H3PP3DSNGTDFBUHO6NUFPQ6FAPMCKEJ6EHDKX2CV2IM
    val clientKP =
      KeyPair.fromSecretSeed("SAUNXQPM7VDH3WMDRHJ2WIN27KD23XD4AZPE62V76Q2SJPXR3DQWEOPX")
    val mockHorizon = MockWebServer()
    mockHorizon.start()

    mockHorizon.enqueue(
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody(testAccountWithNoncompliantSigner)
    )
    val mockHorizonUrl = mockHorizon.url("").toString()

    // 2 ------ Create Test Transaction

    // serverKP does not exist in the network.
    val serverWebAuthDomain = TEST_HOME_DOMAIN
    // GDFWZYGUNUFW4H3PP3DSNGTDFBUHO6NUFPQ6FAPMCKEJ6EHDKX2CV2IM
    val serverKP = KeyPair.random()

    // clientDomainKP does not exist in the network. It refers to the wallet (like Lobstr's)
    // account.
    val clientDomainKP = KeyPair.random()

    val nonce = ByteArray(48)
    val random = SecureRandom()
    random.nextBytes(nonce)
    val base64Encoding = BaseEncoding.base64()
    val encodedNonce = base64Encoding.encode(nonce).toByteArray()

    val sourceAccount = Account(serverKP.accountId, -1L)
    val op1DomainNameMandatory =
      ManageDataOperation.Builder("$serverWebAuthDomain auth", encodedNonce)
        .setSourceAccount(clientKP.accountId)
        .build()
    val op2WebAuthDomainMandatory =
      ManageDataOperation.Builder("web_auth_domain", serverWebAuthDomain.toByteArray())
        .setSourceAccount(serverKP.accountId)
        .build()
    val op3clientDomainOptional =
      ManageDataOperation.Builder("client_domain", "lobstr.co".toByteArray())
        .setSourceAccount(clientDomainKP.accountId)
        .build()

    val transaction =
      TransactionBuilder(AccountConverter.enableMuxed(), sourceAccount, Network.TESTNET)
        .addTimeBounds(TimeBounds.expiresAfter(900))
        .setBaseFee(100)
        .addOperation(op1DomainNameMandatory)
        .addOperation(op2WebAuthDomainMandatory)
        .addOperation(op3clientDomainOptional)
        .build()

    transaction.sign(serverKP)
    transaction.sign(clientDomainKP)
    transaction.sign(clientKP)

    // 2 ------ Create Services
    every { sep10Config.signingSeed } returns String(serverKP.secretSeed)
    every { appConfig.horizonUrl } returns mockHorizonUrl
    every { appConfig.stellarNetworkPassphrase } returns TEST_NETWORK_PASS_PHRASE
    val horizon = Horizon(appConfig)
    this.sep10Service = Sep10Service(appConfig, sep10Config, horizon, jwtService)

    // 3 ------ Run tests
    val validationRequest = ValidationRequest.of(transaction.toEnvelopeXdrBase64())
    assertDoesNotThrow { sep10Service.validateChallenge(validationRequest) }
  }

  @ParameterizedTest
  @CsvSource(
    value = ["true,test.client.stellar.org", "false,test.client.stellar.org", "false,null"]
  )
  fun `test create challenge ok`(clientAttributionRequired: String, clientDomain: String) {
    every { sep10Config.isClientAttributionRequired } returns clientAttributionRequired.toBoolean()
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(TEST_CLIENT_DOMAIN)
        .build()
    cr.clientDomain = if (clientDomain == "null") null else clientDomain

    val challengeResponse = sep10Service.createChallenge(cr)

    assertEquals(challengeResponse.networkPassphrase, TEST_NETWORK_PASS_PHRASE)
    verify(exactly = 1) {
      Sep10Challenge.newChallenge(
        any(),
        Network(TEST_NETWORK_PASS_PHRASE),
        TEST_ACCOUNT,
        TEST_HOME_DOMAIN,
        "test.stellar.org",
        any(),
        any(),
        any(),
        any()
      )
    }
  }

  private fun createTestChallenge(clientDomain: String, signWithClientDomain: Boolean): String {
    val now = System.currentTimeMillis() / 1000L
    val signer = KeyPair.fromSecretSeed(TEST_SIGNING_SEED)
    val memo = MemoId(TEST_MEMO.toLong())
    val txn =
      Sep10Challenge.newChallenge(
        signer,
        Network(TEST_NETWORK_PASS_PHRASE),
        clientKeyPair.accountId,
        TEST_HOME_DOMAIN,
        TEST_HOME_DOMAIN,
        TimeBounds(now, now + 900),
        clientDomain,
        if (clientDomain.isEmpty()) "" else clientDomainKeyPair.accountId,
        memo
      )
    txn.sign(clientKeyPair)
    if (clientDomain.isNotEmpty() && signWithClientDomain) {
      txn.sign(clientDomainKeyPair)
    }
    return txn.toEnvelopeXdrBase64()
  }

  @Test
  fun `test validate challenge when client account is on Stellar network`() {
    val vr = ValidationRequest()
    vr.transaction = createTestChallenge("", false)

    val accountResponse = spyk(AccountResponse(clientKeyPair.accountId, 1))
    val signers =
      arrayOf(TestSigner(clientKeyPair.accountId, "ed25519_public_key", 1, "").toSigner())

    every { accountResponse.signers } returns signers
    every { accountResponse.thresholds.medThreshold } returns 1
    every { horizon.server.accounts().account(ofType(String::class)) } returns accountResponse

    val response = sep10Service.validateChallenge(vr)
    val jwt = jwtService.decode(response.token)
    assertEquals("${clientKeyPair.accountId}:$TEST_MEMO", jwt.sub)
  }

  @Test
  fun `test validate challenge with client domain`() {
    val accountResponse = spyk(AccountResponse(clientKeyPair.accountId, 1))
    val signers =
      arrayOf(
        TestSigner(clientKeyPair.accountId, "ed25519_public_key", 1, "").toSigner(),
        TestSigner(clientDomainKeyPair.accountId, "ed25519_public_key", 1, "").toSigner()
      )

    every { accountResponse.signers } returns signers
    every { accountResponse.thresholds.medThreshold } returns 1
    every { horizon.server.accounts().account(ofType(String::class)) } returns accountResponse

    val vr = ValidationRequest()
    vr.transaction = createTestChallenge(TEST_CLIENT_DOMAIN, true)

    val validationResponse = sep10Service.validateChallenge(vr)

    val token = jwtService.decode(validationResponse.token)
    assertEquals(token.clientDomain, TEST_CLIENT_DOMAIN)

    // Test when the transaction was not signed by the client domain and the client account exists
    vr.transaction = createTestChallenge(TEST_CLIENT_DOMAIN, false)
    assertThrows<InvalidSep10ChallengeException> { sep10Service.validateChallenge(vr) }

    // Test when the transaction was not signed by the client domain and the client account not
    // exists
    every { horizon.server.accounts().account(ofType(String::class)) } answers
      {
        throw ErrorResponse(0, "mock error")
      }
    vr.transaction = createTestChallenge(TEST_CLIENT_DOMAIN, false)

    assertThrows<InvalidSep10ChallengeException> { sep10Service.validateChallenge(vr) }
  }

  @Test
  fun `test validate challenge when client account is not on network`() {
    val vr = ValidationRequest()
    vr.transaction = createTestChallenge("", false)

    every { horizon.server.accounts().account(ofType(String::class)) } answers
      {
        throw ErrorResponse(0, "mock error")
      }

    sep10Service.validateChallenge(vr)
  }

  @Suppress("CAST_NEVER_SUCCEEDS")
  @Test
  fun `Test validate challenge with bad request`() {
    assertThrows<SepValidationException> {
      sep10Service.validateChallenge(null as? ValidationRequest)
    }

    val vr = ValidationRequest()
    vr.transaction = null
    assertThrows<SepValidationException> { sep10Service.validateChallenge(vr) }
  }

  @Test
  fun `Test bad home domain create challenge failure`() {
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(TEST_CLIENT_DOMAIN)
        .build()
    cr.homeDomain = "bad.homedomain.com"

    assertThrows<SepValidationException> { sep10Service.createChallenge(cr) }
  }

  @ParameterizedTest
  @MethodSource("homeDomains")
  fun `test client domain failures`(homeDomain: String?) {
    every { sep10Config.isClientAttributionRequired } returns true
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(TEST_CLIENT_DOMAIN)
        .build()
    cr.homeDomain = homeDomain
    cr.clientDomain = null

    assertThrows<SepValidationException> { sep10Service.createChallenge(cr) }

    // Test client domain rejection
    cr.clientDomain = TEST_CLIENT_DOMAIN
    every { sep10Config.clientAttributionDenyList } returns listOf(TEST_CLIENT_DOMAIN, "")
    assertThrows<SepNotAuthorizedException> { sep10Service.createChallenge(cr) }

    every { sep10Config.clientAttributionDenyList } returns listOf("")
    every { sep10Config.clientAttributionAllowList } returns listOf("")
    // Test client domain not allowed
    assertThrows<SepNotAuthorizedException> { sep10Service.createChallenge(cr) }
  }

  @Test
  fun `test createChallenge() with bad account`() {
    every { sep10Config.isClientAttributionRequired } returns false
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(TEST_CLIENT_DOMAIN)
        .build()
    cr.account = "GXXX"

    assertThrows<SepValidationException> { sep10Service.createChallenge(cr) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["ABC", "12AB", "-1", "0", Integer.MIN_VALUE.toString()])
  fun `test createChallenge() with bad memo`(badMemo: String) {
    every { sep10Config.isClientAttributionRequired } returns false
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(TEST_CLIENT_DOMAIN)
        .build()
    cr.account = TEST_ACCOUNT
    cr.memo = badMemo

    assertThrows<SepValidationException> { sep10Service.createChallenge(cr) }
  }

  @Test
  fun `test getClientAccountId failure`() {
    mockkStatic(NetUtil::class)
    every { NetUtil.fetch(any()) } returns
      "       NETWORK_PASSPHRASE=\"Public Global Stellar Network ; September 2015\"\n"
    mockkStatic(KeyPair::class)

    assertThrows<SepException> { sep10Service.getClientAccountId(TEST_CLIENT_DOMAIN) }

    every { NetUtil.fetch(any()) } answers { throw IOException("Cannot connect") }
    assertThrows<SepException> { sep10Service.getClientAccountId(TEST_CLIENT_DOMAIN) }

    every { NetUtil.fetch(any()) } returns TEST_CLIENT_TOML
    every { KeyPair.fromAccountId(any()) } answers { throw FormatException("Bad Format") }
    assertThrows<SepException> { sep10Service.getClientAccountId(TEST_CLIENT_DOMAIN) }
  }

  @Test
  fun `test appConfig with bad hostUrl`() {
    every { sep10Config.isClientAttributionRequired } returns false
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(TEST_CLIENT_DOMAIN)
        .build()

    every { appConfig.hostUrl } returns "This is bad URL"

    assertThrows<SepException> { sep10Service.createChallenge(cr) }
  }

  @Test
  fun `test createChallenge signing error`() {
    every { sep10Config.isClientAttributionRequired } returns false
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(TEST_CLIENT_DOMAIN)
        .build()

    every {
      Sep10Challenge.newChallenge(any(), any(), any(), any(), any(), any(), any(), any(), any())
    } answers { throw InvalidSep10ChallengeException("mock exception") }

    assertThrows<SepException> { sep10Service.createChallenge(cr) }
  }

  @Test
  fun `test createChallenge() ok when isRequireKnownOmnibusAccount is enabled`() {
    every { sep10Config.isRequireKnownOmnibusAccount } returns true
    every { sep10Config.omnibusAccountList } returns listOf(TEST_ACCOUNT)
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(null)
        .build()

    assertDoesNotThrow { sep10Service.createChallenge(cr) }
    verify(exactly = 1) { sep10Config.isRequireKnownOmnibusAccount }
    verify(exactly = 2) { sep10Config.omnibusAccountList }
  }

  @Test
  fun `Test createChallenge() when isRequireKnownOmnibusAccount is not enabled`() {
    every { sep10Config.isRequireKnownOmnibusAccount } returns false
    every { sep10Config.omnibusAccountList } returns
      listOf("G321E23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP")
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(null)
        .build()

    assertDoesNotThrow { sep10Service.createChallenge(cr) }
    verify(exactly = 1) { sep10Config.isRequireKnownOmnibusAccount }
    verify(exactly = 2) { sep10Config.omnibusAccountList }
  }

  @Test
  fun `test createChallenge() failure when isRequireKnownOmnibusAccount is enabled and account mis-match`() {
    every { sep10Config.isRequireKnownOmnibusAccount } returns true
    every { sep10Config.omnibusAccountList } returns
      listOf("G321E23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP")
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(null)
        .build()

    val ex = assertThrows<SepException> { sep10Service.createChallenge(cr) }
    verify(exactly = 1) { sep10Config.isRequireKnownOmnibusAccount }
    verify(exactly = 2) { sep10Config.omnibusAccountList }
    assertInstanceOf(SepNotAuthorizedException::class.java, ex)
    assertEquals("unable to process", ex.message)
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        "https://test.stellar.org,test.stellar.org",
        "http://test.stellar.org,test.stellar.org",
        "https://test.stellar.org:9800,test.stellar.org:9800",
        "http://test.stellar.org:9800,test.stellar.org:9800",
      ]
  )
  fun `test getDomain from uri`(testUri: String, compareDomain: String) {
    val domain = sep10Service.getDomainFromURI(testUri)
    assertEquals(domain, compareDomain)
  }
}
