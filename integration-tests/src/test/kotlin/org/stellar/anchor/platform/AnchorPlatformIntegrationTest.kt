package org.stellar.anchor.platform

import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.context.ConfigurableApplicationContext
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.callback.GetRateRequest.Type.*
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep38.Sep38Context.SEP31
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.Sep10Config
import org.stellar.anchor.config.Sep1Config
import org.stellar.anchor.config.Sep38Config
import org.stellar.anchor.platform.callback.RestCustomerIntegration
import org.stellar.anchor.platform.callback.RestFeeIntegration
import org.stellar.anchor.platform.callback.RestRateIntegration
import org.stellar.anchor.reference.AnchorReferenceServer
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformIntegrationTest {
  companion object {
    private const val SEP_SERVER_PORT = 8080
    private const val REFERENCE_SERVER_PORT = 8081
    private const val OBSERVER_HEALTH_SERVER_PORT = 8083
    private const val PLATFORM_TO_ANCHOR_SECRET = "myPlatformToAnchorSecret"
    private const val JWT_EXPIRATION_MILLISECONDS: Long = 10000
    private const val FIAT_USD = "iso4217:USD"
    private const val STELLAR_USD =
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"

    private val platformToAnchorJwtService = JwtService(PLATFORM_TO_ANCHOR_SECRET)
    private val authHelper =
      AuthHelper.forJwtToken(
        platformToAnchorJwtService,
        JWT_EXPIRATION_MILLISECONDS,
        "http://localhost:$SEP_SERVER_PORT"
      )

    private lateinit var toml: Sep1Helper.TomlContent
    private lateinit var jwt: String
    private val httpClient: OkHttpClient =
      OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()
    private val gson: Gson = GsonUtils.getInstance()
    private val rci =
      RestCustomerIntegration(
        "http://localhost:$REFERENCE_SERVER_PORT",
        httpClient,
        authHelper,
        gson
      )
    private val rriClient =
      RestRateIntegration("http://localhost:$REFERENCE_SERVER_PORT", httpClient, authHelper, gson)
    private val rfiClient =
      RestFeeIntegration("http://localhost:$REFERENCE_SERVER_PORT", httpClient, authHelper, gson)

    private lateinit var platformServerContext: ConfigurableApplicationContext
    init {
      val props = System.getProperties()
      props.setProperty("REFERENCE_SERVER_CONFIG", "classpath:/anchor-reference-server.yaml")
    }

    @BeforeAll
    @JvmStatic
    fun setup() {
      val envMap =
        mapOf(
          "stellar_anchor_config" to "classpath:integration-test.anchor-config.yaml",
          "secret.sep10.jwt_secret" to "secret",
          "secret.sep10.signing_seed" to "SAKXNWVTRVR4SJSHZUDB2CLJXEQHRT62MYQWA2HBB7YBOTCFJJJ55BZF",
          "secret.data.username" to "user1",
          "secret.data.password" to "password"
        )

      platformServerContext = ServiceRunner.startSepServer(SEP_SERVER_PORT, "/", envMap)
      ServiceRunner.startStellarObserver(OBSERVER_HEALTH_SERVER_PORT, "/", envMap)

      AnchorReferenceServer.start(REFERENCE_SERVER_PORT, "/")
    }
  }

  private fun readSep1Toml(): Sep1Helper.TomlContent {
    val tomlString = resourceAsString("http://localhost:$SEP_SERVER_PORT/.well-known/stellar.toml")
    return Sep1Helper.parse(tomlString)
  }

  @Test
  @Order(1)
  fun runSep1Test() {
    toml = readSep1Toml()
  }

  @Test
  @Order(2)
  fun runSep10Test() {
    jwt = sep10TestAll(toml)
  }

  @Test
  @Order(3)
  fun runSep12Test() {
    sep12TestAll(toml, jwt)
  }

  @Test
  @Order(4)
  fun runSep24Test() {
    sep24TestAll(toml, jwt)
  }

  @Test
  @Order(5)
  fun runSep31Test() {
    sep31TestAll(toml, jwt)
  }

  @Test
  @Order(6)
  fun runSep38Test() {
    sep38TestAll(toml, jwt)
  }

  @Test
  @Order(7)
  fun runPlatformTest() {
    platformTestAll(toml, jwt)
  }

  @Test
  @Order(8)
  fun runSep31UnhappyPath() {
    testSep31UnhappyPath()
  }

  @Test
  fun testCustomerIntegration() {
    assertThrows<NotFoundException> {
      rci.getCustomer(Sep12GetCustomerRequest.builder().id("1").build())
    }
  }

  @Test
  fun testRate_indicativePrices() {
    val result =
      rriClient.getRate(
        GetRateRequest.builder()
          .type(INDICATIVE_PRICES)
          .sellAsset(FIAT_USD)
          .sellAmount("100")
          .buyAsset(STELLAR_USD)
          .build()
      )
    assertNotNull(result)
    val wantBody =
      """{
      "rate":{
        "price":"1.02",
        "sell_amount": "100",
        "buy_amount": "98.0392"
      }
    }""".trimMargin()
    JSONAssert.assertEquals(wantBody, gson.toJson(result), true)
  }

  @Test
  fun testRate_indicativePrice() {
    val result =
      rriClient.getRate(
        GetRateRequest.builder()
          .type(INDICATIVE_PRICE)
          .context(SEP31)
          .sellAsset(FIAT_USD)
          .sellAmount("100")
          .buyAsset(STELLAR_USD)
          .build()
      )
    assertNotNull(result)
    val wantBody =
      """{
      "rate":{
        "total_price":"1.0303032801",
        "price":"1.0200002473",
        "sell_amount": "100",
        "buy_amount": "97.0588",
        "fee": {
          "total": "1.00",
          "asset": "$FIAT_USD",
          "details": [
            {
              "name": "Sell fee",
              "description": "Fee related to selling the asset.",
              "amount": "1.00"
            }
          ]
        }
      }
    }""".trimMargin()
    JSONAssert.assertEquals(wantBody, gson.toJson(result), true)
  }

  @Test
  fun testRate_firm() {
    val rate =
      rriClient.getRate(
          GetRateRequest.builder()
            .type(FIRM)
            .context(SEP31)
            .sellAsset(FIAT_USD)
            .buyAsset(STELLAR_USD)
            .buyAmount("100")
            .build()
        )
        .rate
    assertNotNull(rate)

    // check if id is a valid UUID
    val id = rate.id
    assertDoesNotThrow { UUID.fromString(id) }
    var gotExpiresAt: Instant? = null
    val expiresAtStr = rate.expiresAt.toString()
    assertDoesNotThrow {
      gotExpiresAt = DateTimeFormatter.ISO_INSTANT.parse(rate.expiresAt.toString(), Instant::from)
    }

    val wantExpiresAt =
      ZonedDateTime.now(ZoneId.of("UTC"))
        .plusDays(1)
        .withHour(12)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
    assertEquals(wantExpiresAt.toInstant(), gotExpiresAt)

    // check if rate was persisted by getting the rate with ID
    val gotQuote = rriClient.getRate(GetRateRequest.builder().id(rate.id).build())
    assertEquals(rate.id, gotQuote.rate.id)
    assertEquals("1.02", gotQuote.rate.price)

    val wantBody =
      """{
      "rate":{
        "id": "$id",
        "total_price":"1.03",
        "price":"1.02",
        "sell_amount": "103",
        "buy_amount": "100",
        "expires_at": "$expiresAtStr",
        "fee": {
          "total": "1.00",
          "asset": "$FIAT_USD",
          "details": [
            {
              "name": "Sell fee",
              "description": "Fee related to selling the asset.",
              "amount": "1.00"
            }
          ]
        }
      }
    }""".trimMargin()
    JSONAssert.assertEquals(wantBody, gson.toJson(gotQuote), true)
  }

  @Test
  fun testGetFee() {
    // Create sender customer
    val senderCustomerRequest =
      GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
    val senderCustomer = sep12Client.putCustomer(senderCustomerRequest)

    // Create receiver customer
    val receiverCustomerRequest =
      GsonUtils.getInstance().fromJson(testCustomer2Json, Sep12PutCustomerRequest::class.java)
    val receiverCustomer = sep12Client.putCustomer(receiverCustomerRequest)

    val result =
      rfiClient.getFee(
        GetFeeRequest.builder()
          .sendAmount("10")
          .sendAsset("USDC")
          .receiveAsset("USDC")
          .senderId(senderCustomer!!.id)
          .receiverId(receiverCustomer!!.id)
          .clientId("<client-id>")
          .build()
      )

    assertNotNull(result)
    JSONAssert.assertEquals(
      gson.toJson(result),
      """{
        "fee": {
          "asset": "USDC",
          "amount": "0.30"
        }
      }""",
      true
    )
  }

  @Test
  fun testAppConfig() {
    val appConfig = platformServerContext.getBean(AppConfig::class.java)
    assertEquals("Test SDF Network ; September 2015", appConfig.stellarNetworkPassphrase)
    assertEquals("http://localhost:8080", appConfig.hostUrl)
    assertEquals(listOf("en"), appConfig.languages)
    assertEquals("https://horizon-testnet.stellar.org", appConfig.horizonUrl)
  }

  @Test
  fun testSep1Config() {
    val sep1Config = platformServerContext.getBean(Sep1Config::class.java)
    assertEquals(true, sep1Config.isEnabled)
  }

  @Test
  fun testSep10Config() {
    val sep10Config = platformServerContext.getBean(Sep10Config::class.java)
    assertEquals(true, sep10Config.enabled)
    assertEquals("localhost:8080", sep10Config.homeDomain)
    assertEquals(false, sep10Config.isClientAttributionRequired)
    assertEquals(listOf("lobstr.co", "preview.lobstr.co"), sep10Config.clientAttributionAllowList)
    assertEquals(900, sep10Config.authTimeout)
    assertEquals(86400, sep10Config.jwtTimeout)
  }

  @Test
  fun testSep38Config() {
    val sep38Config = platformServerContext.getBean(Sep38Config::class.java)
    assertEquals(true, sep38Config.isEnabled)
  }

  @Test
  fun testStellarObserverHealth() {
    val httpRequest =
      Request.Builder()
        .url("http://localhost:$OBSERVER_HEALTH_SERVER_PORT/health")
        .header("Content-Type", "application/json")
        .get()
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertEquals(200, response.code)

    val responseBody = gson.fromJson(response.body!!.string(), HashMap::class.java)
    assertEquals(5, responseBody.size)
    assertNotNull(responseBody["started_at"])
    assertNotNull(responseBody["elapsed_time_ms"])
    assertNotNull(responseBody["number_of_checks"])
    assertEquals(2.0, responseBody["number_of_checks"])
    assertNotNull(responseBody["version"])
    assertNotNull(responseBody["checks"])

    val checks = responseBody["checks"] as Map<*, *>

    //    assertEquals(2, checks.size)
    //    assertNotNull(checks["config"])
    //    assertNotNull(checks["stellar_payment_observer"])

    assertEquals(1, checks.size)

    val stellarPaymentObserverCheck = checks["stellar_payment_observer"] as Map<*, *>
    assertEquals(2, stellarPaymentObserverCheck.size)
    assertEquals("GREEN", stellarPaymentObserverCheck["status"])

    val observerStreams = stellarPaymentObserverCheck["streams"] as List<*>
    assertEquals(1, observerStreams.size)

    val stream1 = observerStreams[0] as Map<*, *>
    assertEquals(5, stream1.size)
    assertEquals(false, stream1["thread_shutdown"])
    assertEquals(false, stream1["thread_terminated"])
    assertEquals(false, stream1["stopped"])
    assertNotNull(stream1["last_event_id"])
  }
}
