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
import org.springframework.boot.SpringApplication
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
    private const val CUSTODY_SERVER_SECRET = "custodyServerSecret"
    private const val PLATFORM_SERVER_PORT = 8080
    private const val CUSTODY_SERVER_SERVER_PORT = 8085
  }
  private val gson = GsonUtils.getInstance()
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.MINUTES)
      .readTimeout(10, TimeUnit.MINUTES)
      .writeTimeout(10, TimeUnit.MINUTES)
      .build()
  private lateinit var platformServerContext: ConfigurableApplicationContext
  private lateinit var custodyServerContext: ConfigurableApplicationContext
  private lateinit var mockBusinessServer: MockWebServer

  @BeforeAll
  fun setup() {
    // mock Anchor backend
    mockBusinessServer = MockWebServer()
    mockBusinessServer.start()

    val envMap = readResourceAsMap("profiles/default/config.env")
    envMap["data.type"] = "h2"
    envMap["events.enabled"] = "false"
    envMap["assets.value"] = getResourceFile("config/assets.yaml").absolutePath
    envMap["sep1.toml.value"] = getResourceFile("config/stellar.toml").absolutePath

    envMap["callback_api.base_url"] = mockBusinessServer.url("").toString()
    envMap["platform_api.auth.type"] = "api_key"
    envMap["secret.platform_api.auth_secret"] = ANCHOR_TO_PLATFORM_SECRET
    envMap["callback_api.auth.type"] = "api_key"
    envMap["secret.callback_api.auth_secret"] = PLATFORM_TO_ANCHOR_SECRET
    envMap["custody_server.auth.type"] = "api_key"
    envMap["secret.custody_server.auth_secret"] = CUSTODY_SERVER_SECRET
    envMap["secret.custody.fireblocks.api_key"] = "testApiKey"
    envMap["secret.custody.fireblocks.secret_key"] =
      getResourceFile("config/secret_key.txt").readText()

    // Start platform
    platformServerContext = AnchorPlatformServer().start(envMap)

    // Start custody server
    custodyServerContext = CustodyServer().start(envMap)
  }

  @AfterAll
  fun teardown() {
    SpringApplication.exit(platformServerContext)
    SpringApplication.exit(custodyServerContext)
    mockBusinessServer.shutdown()
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
        "GET,/exchange/quotes/id"
      ]
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
        "GET,/exchange/quotes/id"
      ]
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

  @ParameterizedTest
  @CsvSource(value = ["POST,/transactions"])
  fun test_incomingCustodyAuth_emptyApiKey_authFails(method: String, endpoint: String) {
    val httpRequest =
      Request.Builder()
        .url("http://localhost:$CUSTODY_SERVER_SERVER_PORT$endpoint")
        .header("Content-Type", "application/json")
        .method(method, getDummyRequestBody(method))
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertEquals(403, response.code)
  }

  @ParameterizedTest
  @CsvSource(value = ["POST,/transactions"])
  fun test_incomingCustodyAuth_emptyApiKey_authPasses(method: String, endpoint: String) {
    val httpRequest =
      Request.Builder()
        .url("http://localhost:$CUSTODY_SERVER_SERVER_PORT$endpoint")
        .header("Content-Type", "application/json")
        .header("X-Api-Key", ANCHOR_TO_PLATFORM_SECRET)
        .method(method, getDummyRequestBody(method))
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertEquals(403, response.code)
  }

  @Test
  fun test_ApiAuthIsBeingAddedInPlatformToAnchorRequests() {
    // check if at least one outgoing call is carrying the auth header.
    mockBusinessServer.enqueue(
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
    }"""
            .trimMargin()
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

    val request = mockBusinessServer.takeRequest()
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
        """
        .replace("\n        ", "")
    MatcherAssert.assertThat(request.path, CoreMatchers.endsWith(wantEndpoint))
    assertEquals("", request.body.readUtf8())
  }

  private fun getDummyRequestBody(method: String): RequestBody? {
    return if (method != "PATCH" && method != "POST") null
    else OkHttpUtil.buildJsonRequestBody(gson.toJson(mapOf("proposedAssetsJson" to "bar")))
  }
}
