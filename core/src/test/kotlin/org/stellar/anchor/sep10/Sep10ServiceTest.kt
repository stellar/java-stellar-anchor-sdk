package org.stellar.anchor.sep10

import com.google.gson.Gson
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.Sep10Config
import org.stellar.anchor.dto.sep10.ChallengeRequest
import org.stellar.anchor.dto.sep10.ChallengeRequestTest
import org.stellar.anchor.dto.sep10.ValidationRequest
import org.stellar.anchor.exception.SepException
import org.stellar.anchor.exception.SepValidationException
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.util.NetUtil
import org.stellar.sdk.*
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.responses.AccountResponse
import shadow.com.google.gson.annotations.SerializedName
import java.io.IOException
import java.util.stream.Stream

@Suppress("unused")
internal class TestSigner(
    @SerializedName("key")
    val key: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("weight")
    val weight: Int,

    @SerializedName("sponsor")
    val sponsor: String
) {
    fun toSigner(): AccountResponse.Signer {
        val gson = Gson()
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
    }

    @MockK(relaxed = true)
    private lateinit var appConfig: AppConfig
    @MockK(relaxed = true)
    private lateinit var sep10Config: Sep10Config
    @MockK(relaxed = true)
    private lateinit var horizon: Horizon

    private lateinit var jwtService: JwtService
    private lateinit var sep10Service: Sep10Service
    private val clientKeyPair = KeyPair.random()
    private val clientDomainKeyPair = KeyPair.random()

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { sep10Config.signingSeed } returns TEST_SIGNING_SEED
        every { sep10Config.homeDomain } returns TEST_HOME_DOMAIN
        every { sep10Config.clientAttributionDenyList } returns listOf("")
        every { sep10Config.clientAttributionAllowList } returns listOf(TEST_CLIENT_DOMAIN)
        every { sep10Config.authTimeout } returns 900
        every { sep10Config.jwtTimeout } returns 900

        every { appConfig.stellarNetworkPassPhrase } returns TEST_NETWORK_PASS_PHRASE
        every { appConfig.hostUrl } returns TEST_HOST_URL
        every { appConfig.jwtSecretKey } returns TEST_JWT_SECRET


        mockkStatic(NetUtil::class)
        mockkStatic(Sep10Challenge::class)

        every { NetUtil.fetch(any()) } returns TEST_CLIENT_TOML

        this.jwtService = spyk(JwtService(appConfig))
        this.sep10Service = Sep10Service(appConfig, sep10Config, horizon, jwtService)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "true,test.client.stellar.org",
            "false,test.client.stellar.org",
            "false,null"]
    )
    fun testOkCreateChallenge(clientAttributionRequired: String, clientDomain: String) {
        every { sep10Config.isClientAttributionRequired } returns clientAttributionRequired.toBoolean()
        val cr = ChallengeRequest.of(TEST_ACCOUNT, TEST_MEMO, TEST_HOME_DOMAIN, TEST_CLIENT_DOMAIN)
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
                any()
            )
        }
    }

    private fun createTestChallenge(clientDomain: String, signWithClientDomain: Boolean): String {
        val now = System.currentTimeMillis() / 1000L
        val signer = KeyPair.fromSecretSeed(TEST_SIGNING_SEED)
        val txn = Sep10Challenge.newChallenge(
            signer,
            Network(TEST_NETWORK_PASS_PHRASE),
            clientKeyPair.accountId,
            TEST_HOME_DOMAIN,
            TEST_HOME_DOMAIN,
            TimeBounds(now, now + 900),
            clientDomain,
            if (clientDomain.isEmpty()) "" else clientDomainKeyPair.accountId
        )
        txn.sign(clientKeyPair)
        if (clientDomain.isNotEmpty() && signWithClientDomain) {
            txn.sign(clientDomainKeyPair)
        }
        return txn.toEnvelopeXdrBase64()
    }

    @Test
    fun testOkValidateChallengeClientAccountOnNetwork() {
        val vr = ValidationRequest()
        vr.transaction = createTestChallenge("", false)

        val accountResponse = spyk(AccountResponse(clientKeyPair.accountId, 1))
        val signers = Array<AccountResponse.Signer>(1) {
            TestSigner(
                clientKeyPair.accountId,
                "ed25519_public_key",
                1,
                ""
            ).toSigner()
        }

        every { accountResponse.signers } returns signers
        every { accountResponse.thresholds.medThreshold } returns 1
        every { horizon.server.accounts().account(ofType(String::class)) } returns accountResponse

        sep10Service.validateChallenge(vr)
    }

    @Test
    fun testValidateChallengeWithClientDomain() {
        val accountResponse = spyk(AccountResponse(clientKeyPair.accountId, 1))
        val signers = arrayOf(
            TestSigner(
                clientKeyPair.accountId,
                "ed25519_public_key",
                1,
                ""
            ).toSigner(),
            TestSigner(
                clientDomainKeyPair.accountId,
                "ed25519_public_key",
                1,
                ""
            ).toSigner()
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
        assertThrows<InvalidSep10ChallengeException> {
            sep10Service.validateChallenge(vr)
        }

        // Test when the transaction was not signed by the client domain and the client account not exists
        every { horizon.server.accounts().account(ofType(String::class)) } answers {
            throw ErrorResponse(0, "mock error")
        }
        vr.transaction = createTestChallenge(TEST_CLIENT_DOMAIN, false)

        assertThrows<InvalidSep10ChallengeException> {
            sep10Service.validateChallenge(vr)
        }
    }


    @Test
    fun testOkValidateChallengeClientAccountNotOnNetwork() {
        val vr = ValidationRequest()
        vr.transaction = createTestChallenge("", false)

        every { horizon.server.accounts().account(ofType(String::class)) } answers {
            throw ErrorResponse(0, "mock error")
        }

        sep10Service.validateChallenge(vr)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    @Test
    fun testErrValidateChallengeBadRequest() {
        assertThrows<SepValidationException> {
            sep10Service.validateChallenge(null as? ValidationRequest)
        }

        val vr = ValidationRequest()
        vr.transaction = null
        assertThrows<SepValidationException> {
            sep10Service.validateChallenge(vr)
        }

    }

    @Test
    fun testErrBadHomeDomainCreateChallenge() {
        val cr = ChallengeRequest.of(TEST_ACCOUNT, TEST_MEMO, TEST_HOME_DOMAIN, TEST_CLIENT_DOMAIN)
        cr.homeDomain = "bad.homedomain.com"

        assertThrows<SepValidationException> {
            sep10Service.createChallenge(cr)
        }
    }

    @ParameterizedTest
    @MethodSource("homeDomains")
    fun testClientDomainFailure(homeDomain: String?) {
        every { sep10Config.isClientAttributionRequired } returns true
        val cr = ChallengeRequest.of(TEST_ACCOUNT, TEST_MEMO, TEST_HOME_DOMAIN, TEST_CLIENT_DOMAIN)
        cr.homeDomain = homeDomain
        cr.clientDomain = null

        assertThrows<SepValidationException> {
            sep10Service.createChallenge(cr)
        }

        // Test client domain rejection
        cr.clientDomain = TEST_CLIENT_DOMAIN
        every { sep10Config.clientAttributionDenyList } returns listOf(TEST_CLIENT_DOMAIN, "")
        assertThrows<SepValidationException> {
            sep10Service.createChallenge(cr)
        }

        every { sep10Config.clientAttributionDenyList } returns listOf("")
        every { sep10Config.clientAttributionAllowList } returns listOf("")
        // Test client domain not allowed
        assertThrows<SepValidationException> {
            sep10Service.createChallenge(cr)
        }
    }

    @Test
    fun testBadAccount() {
        every { sep10Config.isClientAttributionRequired } returns false
        val cr = ChallengeRequest.of(TEST_ACCOUNT, TEST_MEMO, TEST_HOME_DOMAIN, TEST_CLIENT_DOMAIN)
        cr.account = "GXXX"

        assertThrows<SepValidationException> {
            sep10Service.createChallenge(cr)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["ABC", "12AB", "-1", "0", Integer.MIN_VALUE.toString()])
    fun testBadMemo(badMemo: String) {
        every { sep10Config.isClientAttributionRequired } returns false
        val cr = ChallengeRequest.of(ChallengeRequestTest.TEST_ACCOUNT, TEST_MEMO, TEST_HOME_DOMAIN, TEST_CLIENT_DOMAIN)
        cr.account = TEST_ACCOUNT
        cr.memo = badMemo

        assertThrows<SepValidationException> {
            sep10Service.createChallenge(cr)
        }
    }

    @Test
    fun testGetClientAccountIdFailure() {
        mockkStatic(NetUtil::class)
        every { NetUtil.fetch(any()) } returns "       NETWORK_PASSPHRASE=\"Public Global Stellar Network ; September 2015\"\n"
        mockkStatic(KeyPair::class)

        assertThrows<SepException> {
            sep10Service.getClientAccountId(TEST_CLIENT_DOMAIN)
        }

        every { NetUtil.fetch(any()) } answers {
            throw IOException("Cannot connect")
        }
        assertThrows<SepException> {
            sep10Service.getClientAccountId(TEST_CLIENT_DOMAIN)
        }

        every { NetUtil.fetch(any()) } returns TEST_CLIENT_TOML
        every { KeyPair.fromAccountId(any()) } answers {
            throw FormatException("Bad Format")
        }
        assertThrows<SepException> {
            sep10Service.getClientAccountId(TEST_CLIENT_DOMAIN)
        }
    }

    @Test
    fun testAppConfigBadHostURL() {
        every { sep10Config.isClientAttributionRequired } returns false
        val cr = ChallengeRequest.of(TEST_ACCOUNT, TEST_MEMO, TEST_HOME_DOMAIN, TEST_CLIENT_DOMAIN)

        every {
            appConfig.hostUrl
        } returns "This is bad URL"

        assertThrows<SepException> {
            sep10Service.createChallenge(cr)
        }
    }

    @Test
    fun testCreateChallengeSigningError() {
        every { sep10Config.isClientAttributionRequired } returns false
        val cr = ChallengeRequest.of(TEST_ACCOUNT, TEST_MEMO, TEST_HOME_DOMAIN, TEST_CLIENT_DOMAIN)

        every {
            Sep10Challenge.newChallenge(any(), any(), any(), any(), any(), any(), any(), any())
        } answers { throw InvalidSep10ChallengeException("mock exception") }

        assertThrows<SepException> {
            sep10Service.createChallenge(cr)
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "https://test.stellar.org,test.stellar.org",
            "http://test.stellar.org,test.stellar.org",
            "https://test.stellar.org:9800,test.stellar.org:9800",
            "http://test.stellar.org:9800,test.stellar.org:9800",
        ]
    )
    fun testGetDomainFromURI(testUri: String, compareDomain: String) {
        val domain = sep10Service.getDomainFromURI(testUri)
        assertEquals(domain, compareDomain)
    }
}
