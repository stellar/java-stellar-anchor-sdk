package org.stellar.anchor.platform

import com.google.gson.Gson
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.integration.rate.GetRateRequest
import org.stellar.anchor.integration.rate.GetRateResponse
import org.stellar.anchor.util.OkHttpUtil

class PlatformRateIntegrationTest {
  private lateinit var server: MockWebServer
  private lateinit var rateIntegration: PlatformRateIntegration
  private val gson = Gson()

  @BeforeEach
  @Throws(IOException::class)
  fun setUp() {
    server = MockWebServer()
    server.start()
    rateIntegration = PlatformRateIntegration(server.url("").toString(), OkHttpUtil.buildClient())
  }

  private fun getRateResponse(price: String, expiresAt: LocalDateTime?): MockResponse {
    var expiresAtStr: String? = null
    if (expiresAt != null) {
      expiresAtStr = expiresAt.format(DateTimeFormatter.ISO_DATE_TIME)
    }
    return getRateResponse(price, expiresAtStr)
  }

  private fun getRateResponse(price: String, expiresAt: String? = null): MockResponse {
    val bodyMap = hashMapOf("price" to price)
    if (expiresAt != null) {
      bodyMap["expires_at"] = expiresAt
    }
    return MockResponse()
      .addHeader("Content-Type", "application/json")
      .setResponseCode(200)
      .setBody(gson.toJson(bodyMap))
  }

  @Test
  fun test_getRate_endpoint() {
    val testGetRate = { endpoint: String, getRateRequest: GetRateRequest ->
      server.enqueue(getRateResponse("1"))

      val getRateResponse = rateIntegration.getRate(getRateRequest)
      val wantResponse = GetRateResponse("1")
      assertEquals(wantResponse, getRateResponse)

      val request = server.takeRequest()
      assertEquals("GET", request.method)
      assertEquals("application/json", request.headers["Content-Type"])
      assertEquals(null, request.headers["Authorization"])
      MatcherAssert.assertThat(request.path, CoreMatchers.endsWith(endpoint))
    }

    val builder = GetRateRequest.builder()

    // no parameters
    var getRateRequest = builder.build()
    testGetRate("/rate", getRateRequest)

    // getPrices parameters
    getRateRequest =
      builder
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .build()
    testGetRate(
      "/rate?type=indicative&sell_asset=iso4217%3AUSD&sell_amount=100&sell_delivery_method=WIRE",
      getRateRequest
    )

    // all parameters
    getRateRequest =
      builder
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset("iso4217:USD")
        .buyAsset("stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN")
        .sellAmount("100")
        .buyAmount("100")
        .sellDeliveryMethod("WIRE")
        .buyDeliveryMethod("WIRE")
        .clientDomain("test.com")
        .account("GDGWTSQKQQAT2OXRSFLADMN4F6WJQMPJ5MIOKIZ2AMBYUI67MJA4WRLA")
        .memo("foo")
        .memoType("text")
        .build()
    testGetRate(
      "/rate?type=indicative&sell_asset=iso4217%3AUSD&buy_asset=stellar%3AUSDC%3AGA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN&sell_amount=100&buy_amount=100&sell_delivery_method=WIRE&buy_delivery_method=WIRE&client_domain=test.com&account=GDGWTSQKQQAT2OXRSFLADMN4F6WJQMPJ5MIOKIZ2AMBYUI67MJA4WRLA&memo=foo&memo_type=text",
      getRateRequest
    )
  }
}
