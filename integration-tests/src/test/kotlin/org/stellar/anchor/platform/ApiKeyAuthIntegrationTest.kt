package org.stellar.anchor.platform

import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.context.ConfigurableApplicationContext
import org.stellar.anchor.api.sep.sep38.GetPriceResponse
import org.stellar.anchor.api.sep.sep38.RateFee
import org.stellar.anchor.api.sep.sep38.Sep38Context
import org.stellar.anchor.api.sep.sep38.Sep38GetPriceRequest
import org.stellar.anchor.sep38.Sep38Service
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.OkHttpUtil

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiKeyAuthIntegrationTest {
  companion object {
    private const val ANCHOR_TO_PLATFORM_SECRET = "myAnchorToPlatformSecret"
    private const val PLATFORM_TO_ANCHOR_SECRET = "myPlatformToAnchorSecret"
    private const val PLATFORM_SERVER_PORT = 8888
  }
  private val gson = GsonUtils.getInstance()
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.MINUTES)
      .readTimeout(10, TimeUnit.MINUTES)
      .writeTimeout(10, TimeUnit.MINUTES)
      .build()
  private lateinit var platformServerContext: ConfigurableApplicationContext
  private lateinit var mockAnchor: MockWebServer

  @BeforeAll
  fun setup() {
    // mock Anchor backend
    mockAnchor = MockWebServer()
    mockAnchor.start()
    val mockAnchorUrl = mockAnchor.url("").toString()

    // Start platform
    platformServerContext =
      AnchorPlatformServer.start(
        mapOf(
          "sep_server.port" to PLATFORM_SERVER_PORT,
          "sep_server.context_path" to "/",
          "stellar_anchor_config" to "classpath:integration-test.anchor-config.yaml",
          "secret.sep10.jwt_secret" to "secret",
          "secret.sep10.signing_seed" to "SAKXNWVTRVR4SJSHZUDB2CLJXEQHRT62MYQWA2HBB7YBOTCFJJJ55BZF",
          "platform_api.auth.type" to "API_KEY",
          "secret.platform_api.auth_secret" to ANCHOR_TO_PLATFORM_SECRET,
          "callback_api.base_url" to mockAnchorUrl,
          "callback_api.auth.type" to "API_KEY",
          "secret.callback_api.auth_secret" to PLATFORM_TO_ANCHOR_SECRET
        )
      )
  }

  @AfterAll
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        "GET,/transactions",
        "PATCH,/transactions",
        "GET,/transactions/my_id",
        "GET,/exchange/quotes",
        "GET,/exchange/quotes/id"]
  )
  fun test_incomingPlatformAuth_emptyApiKey_authFails(method: String, endpoint: String) {
    val httpRequest =
      Request.Builder()
        .url("http://localhost:$PLATFORM_SERVER_PORT$endpoint")
        .header("Content-Type", "application/json")
        .method(method, getDummyRequestBody(method))
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertEquals(403, response.code)
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        "GET,/transactions",
        "PATCH,/transactions",
        "GET,/transactions/my_id",
        "GET,/exchange/quotes",
        "GET,/exchange/quotes/id"]
  )
  fun test_incomingPlatformAuth_apiKey_authPasses(method: String, endpoint: String) {
    val httpRequest =
      Request.Builder()
        .url("http://localhost:$PLATFORM_SERVER_PORT$endpoint")
        .header("Content-Type", "application/json")
        .header("X-Api-Key", ANCHOR_TO_PLATFORM_SECRET)
        .method(method, getDummyRequestBody(method))
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertNotEquals(403, response.code)
  }

  @Test
  fun test_ApiAuthIsBeingAddedInPlatformToAnchorRequests() {
    // check if at least one outgoing call is carrying the auth header.
    mockAnchor.enqueue(
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody(
          """{
      "rate": {
        "price": "1.00",
        "total_price": "1.00",
        "sell_amount": "100",
        "buy_amount": "100",
        "fee": {
          "total": "0.00",
          "asset": "iso4217:USD"
        }
      }
    }""".trimMargin()
        )
    )
    val sep38Service = platformServerContext.getBean(Sep38Service::class.java)

    val getPriceRequest =
      Sep38GetPriceRequest.builder()
        .sellAssetName("iso4217:USD")
        .sellAmount("100")
        .buyAssetName("stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP")
        .context(Sep38Context.SEP31)
        .build()
    val gotResponse = sep38Service.getPrice(getPriceRequest)

    val wantResponse =
      GetPriceResponse.builder()
        .price("1.00")
        .totalPrice("1.00")
        .sellAmount("100")
        .buyAmount("100")
        .fee(RateFee("0.00", "iso4217:USD"))
        .build()
    assertEquals(wantResponse, gotResponse)

    val request = mockAnchor.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals(PLATFORM_TO_ANCHOR_SECRET, request.headers["X-Api-Key"])
    assertNull(request.headers["Authorization"])
    val wantEndpoint =
      """/rate
        ?type=indicative_price
        &context=sep31
        &sell_asset=iso4217%3AUSD
        &sell_amount=100
        &buy_asset=stellar%3AUSDC%3AGDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP
        """.replace(
        "\n        ",
        ""
      )
    MatcherAssert.assertThat(request.path, CoreMatchers.endsWith(wantEndpoint))
    assertEquals("", request.body.readUtf8())
  }

  private fun getDummyRequestBody(method: String): RequestBody? {
    return if (method != "PATCH") null
    else OkHttpUtil.buildJsonRequestBody(gson.toJson(mapOf("proposedAssetsJson" to "bar")))
  }
}
