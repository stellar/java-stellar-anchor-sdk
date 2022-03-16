package org.stellar.anchor.platform

import com.google.gson.Gson
import java.io.IOException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.exception.AnchorException
import org.stellar.anchor.exception.BadRequestException
import org.stellar.anchor.exception.NotFoundException
import org.stellar.anchor.exception.ServerErrorException
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

  private fun getRateResponse(price: String, expiresAt: String? = null): MockResponse {
    val bodyMap = hashMapOf("rate" to hashMapOf("price" to price))
    if (expiresAt != null) {
      bodyMap["rate"]!!["expires_at"] = expiresAt
    }
    return MockResponse()
      .addHeader("Content-Type", "application/json")
      .setResponseCode(200)
      .setBody(gson.toJson(bodyMap))
  }

  @Test
  fun test_getRate() {
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
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()
    testGetRate(
      """/rate
        ?type=indicative
        &sell_asset=iso4217%3AUSD
        &sell_amount=100
        &country_code=USA
        &sell_delivery_method=WIRE""".replace(
        "\n        ",
        ""
      ),
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
        .countryCode("USA")
        .expireAfter("2022-04-30T02:15:44.000Z")
        .sellDeliveryMethod("WIRE")
        .buyDeliveryMethod("WIRE")
        .clientDomain("test.com")
        .account("GDGWTSQKQQAT2OXRSFLADMN4F6WJQMPJ5MIOKIZ2AMBYUI67MJA4WRLA")
        .memo("foo")
        .memoType("text")
        .build()
    testGetRate(
      """/rate
        ?type=indicative
        &sell_asset=iso4217%3AUSD
        &buy_asset=stellar%3AUSDC%3AGA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN
        &sell_amount=100
        &buy_amount=100
        &country_code=USA
        &expire_after=2022-04-30T02%3A15%3A44.000Z
        &sell_delivery_method=WIRE
        &buy_delivery_method=WIRE
        &client_domain=test.com
        &account=GDGWTSQKQQAT2OXRSFLADMN4F6WJQMPJ5MIOKIZ2AMBYUI67MJA4WRLA
        &memo=foo
        &memo_type=text""".replace(
        "\n        ",
        ""
      ),
      getRateRequest
    )
  }

  @Test
  fun test_getRate_errorHandling() {
    val validateRequest =
        { statusCode: Int, responseBody: String?, wantException: AnchorException ->
      // mock response
      var mockResponse =
        MockResponse().addHeader("Content-Type", "application/json").setResponseCode(statusCode)
      if (responseBody != null) mockResponse = mockResponse.setBody(responseBody)
      server.enqueue(mockResponse)

      // execute command
      val dummyRequest = GetRateRequest.builder().type(GetRateRequest.Type.INDICATIVE).build()
      val ex = assertThrows<AnchorException> { rateIntegration.getRate(dummyRequest) }

      // validate exception
      assertEquals(wantException, ex)

      // validateRequest
      val request = server.takeRequest()
      assertEquals("GET", request.method)
      assertEquals("application/json", request.headers["Content-Type"])
      assertEquals(null, request.headers["Authorization"])
      MatcherAssert.assertThat(request.path, CoreMatchers.endsWith("/rate?type=indicative"))
      assertEquals("", request.body.readUtf8())
    }

    // 400 without body
    validateRequest(400, null, BadRequestException("Bad Request"))

    // 400 with body
    validateRequest(400, """{"error": "foo 400"}""", BadRequestException("foo 400"))

    // 404 without body
    validateRequest(404, null, NotFoundException("Not Found"))

    // 404 with body
    validateRequest(404, """{"error": "foo 404"}""", NotFoundException("foo 404"))

    // 422 without body
    validateRequest(422, null, BadRequestException("Bad Request"))

    // 422 with body
    validateRequest(422, """{"error": "foo 422"}""", BadRequestException("foo 422"))

    // 500
    validateRequest(500, """{"error": "foo 500"}""", ServerErrorException("internal server error"))

    // 200 with invalid body
    val serverErrorException = ServerErrorException("internal server error")
    validateRequest(200, """{"rate": {"price": "invalid json",}}""", serverErrorException)

    // 200 where getRateResponse is missing a price
    validateRequest(200, """{"rate": "missing price"}""", serverErrorException)
  }
}
