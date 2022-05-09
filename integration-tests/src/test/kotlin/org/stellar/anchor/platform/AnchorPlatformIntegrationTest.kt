package org.stellar.anchor.platform

import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.callback.GetRateRequest.Type.FIRM
import org.stellar.anchor.api.callback.GetRateRequest.Type.INDICATIVE
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest
import org.stellar.anchor.platform.callback.RestCustomerIntegration
import org.stellar.anchor.platform.callback.RestFeeIntegration
import org.stellar.anchor.platform.callback.RestRateIntegration
import org.stellar.anchor.reference.AnchorReferenceServer
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper

class AnchorPlatformIntegrationTest {
  companion object {
    private const val SEP_SERVER_PORT = 8080
    private const val REFERENCE_SERVER_PORT = 8081
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
      RestCustomerIntegration("http://localhost:$REFERENCE_SERVER_PORT", httpClient, gson)
    private val rri =
      RestRateIntegration("http://localhost:$REFERENCE_SERVER_PORT", httpClient, gson)
    private val rfi =
      RestFeeIntegration("http://localhost:$REFERENCE_SERVER_PORT", httpClient, gson)
    const val fiatUSD = "iso4217:USD"
    const val stellarUSDC = "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    init {
      val props = System.getProperties()
      props.setProperty("REFERENCE_SERVER_CONFIG", "classpath:/anchor-reference-server.yaml")
    }
    @BeforeAll
    @JvmStatic
    fun setup() {
      AnchorPlatformServer.start(
        SEP_SERVER_PORT,
        "/",
        mapOf("stellar.anchor.config" to "classpath:/test-anchor-config.yaml")
      )

      AnchorReferenceServer.start(REFERENCE_SERVER_PORT, "/")
    }

    @AfterAll fun tearDown() {}
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
  fun testCustomerIntegration() {
    assertThrows<NotFoundException> {
      rci.getCustomer(Sep12GetCustomerRequest.builder().id("1").build())
    }
  }

  @Test
  fun testRateIndicative() {
    val result =
      rri.getRate(
        GetRateRequest.builder()
          .type(INDICATIVE)
          .sellAsset(fiatUSD)
          .sellAmount("100")
          .buyAsset(stellarUSDC)
          .build()
      )
    Assertions.assertNotNull(result)
    val wantBody = """{"rate":{"price":"1.02"}}"""
    JSONAssert.assertEquals(wantBody, gson.toJson(result), true)
  }

  @Test
  fun testGetFee() {
    val result =
      rfi.getFee(
        GetFeeRequest.builder()
          .sendAmount("10")
          .sendAsset("USDC")
          .receiveAsset("USDC")
          .senderId("sender_id")
          .receiverId("receiver_id")
          .build()
      )

    Assertions.assertNotNull(result)
    JSONAssert.assertEquals(
      gson.toJson(result),
      """
         {
             "fee": {
                "asset": "USDC",
                "amount": "0.30"
             }
         }
      """,
      true
    )
  }

  @Test
  fun testGetRateFirm() {
    val rate =
      rri.getRate(
          GetRateRequest.builder()
            .type(FIRM)
            .sellAsset(fiatUSD)
            .buyAsset(stellarUSDC)
            .buyAmount("100")
            .build()
        )
        .rate
    Assertions.assertNotNull(rate)

    // check if id is a valid UUID
    val id = rate.id
    assertDoesNotThrow { UUID.fromString(id) }
    var gotExpiresAt: Instant? = null
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
    val gotQuote = rri.getRate(GetRateRequest.builder().id(rate.id).build())
    assertEquals(rate.id, gotQuote.rate.id)
    assertEquals("1.02", gotQuote.rate.price)
  }
}
