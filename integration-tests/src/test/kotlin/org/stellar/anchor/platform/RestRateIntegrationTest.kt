package org.stellar.anchor.platform

import io.mockk.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Calendar
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.callback.GetRateRequest.Type.*
import org.stellar.anchor.api.callback.GetRateResponse
import org.stellar.anchor.api.exception.AnchorException
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.exception.ServerErrorException
import org.stellar.anchor.api.sep.sep38.RateFee
import org.stellar.anchor.api.sep.sep38.RateFeeDetail
import org.stellar.anchor.api.sep.sep38.Sep38Context.*
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.JwtToken
import org.stellar.anchor.platform.callback.RestRateIntegration
import org.stellar.anchor.reference.model.Quote
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.OkHttpUtil

class RestRateIntegrationTest {
  companion object {
    private const val PLATFORM_TO_ANCHOR_SECRET = "myPlatformToAnchorSecret"
    private const val JWT_EXPIRATION_MILLISECONDS: Long = 100000000
    private val platformToAnchorJwtService = JwtService(PLATFORM_TO_ANCHOR_SECRET)
    private val authHelper =
      AuthHelper.forJwtToken(
        platformToAnchorJwtService,
        JWT_EXPIRATION_MILLISECONDS,
        "http://localhost:8080"
      )
  }
  private lateinit var server: MockWebServer
  private lateinit var rateIntegration: RestRateIntegration
  private lateinit var mockJwtToken: String
  private val gson = GsonUtils.getInstance()

  @BeforeEach
  fun setUp() {
    server = MockWebServer()
    server.start()
    rateIntegration =
      RestRateIntegration(
        server.url("").toString(),
        OkHttpUtil.buildClient(),
        authHelper,
        GsonUtils.getInstance()
      )

    // Mock calendar to guarantee the jwt token format
    val calendarSingleton = Calendar.getInstance()
    val currentTimeMilliseconds = calendarSingleton.getTimeInMillis()
    mockkObject(calendarSingleton)
    every { calendarSingleton.getTimeInMillis() } returns currentTimeMilliseconds
    every { calendarSingleton.setTimeInMillis(any()) } answers { callOriginal() }
    mockkStatic(Calendar::class)
    every { Calendar.getInstance() } returns calendarSingleton
    // mock jwt token based on the mocked calendar
    val jwtToken =
      JwtToken.of(
        "http://localhost:8080",
        currentTimeMilliseconds / 1000L,
        (currentTimeMilliseconds + JWT_EXPIRATION_MILLISECONDS) / 1000L
      )
    mockJwtToken = platformToAnchorJwtService.encode(jwtToken)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  private fun mockGetRateResponse(
    price: String,
    sellAmount: String,
    buyAmount: String,
    totalPrice: String? = null,
    id: String? = null,
    fee: RateFee? = null,
    expiresAt: String? = null
  ): MockResponse {
    val rate =
      hashMapOf<String, Any>(
        "price" to price,
        "sell_amount" to sellAmount,
        "buy_amount" to buyAmount,
      )

    if (totalPrice != null) {
      rate["total_price"] = totalPrice
    }

    if (fee != null) {
      rate["fee"] = fee
    }

    if (id != null) {
      rate["id"] = id
    }

    if (expiresAt != null) {
      rate["expires_at"] = expiresAt
    }

    val bodyMap = hashMapOf("rate" to rate)
    return MockResponse()
      .addHeader("Content-Type", "application/json")
      .setResponseCode(200)
      .setBody(gson.toJson(bodyMap))
  }

  private fun mockSellAssetFee(sellAsset: String?): RateFee {
    assertNotNull(sellAsset)

    val rateFee = RateFee("0", sellAsset)
    rateFee.addFeeDetail(RateFeeDetail("Sell fee", "2.00"))
    return rateFee
  }

  @Test
  fun test_getRate() {
    val fee = mockSellAssetFee("iso4217:USD")

    val testGetRate = { endpoint: String, getRateRequest: GetRateRequest ->
      server.enqueue(
        mockGetRateResponse(
          "1.0",
          "102",
          "100",
          totalPrice = "1.02",
          id = "my-id",
          expiresAt = "2022-04-30T02:15:44.000Z",
          fee = fee
        ) // This is a dummy response, we're not testing its values
      )

      val getRateResponse = rateIntegration.getRate(getRateRequest)
      assertInstanceOf(GetRateResponse::class.java, getRateResponse)

      val request = server.takeRequest()
      assertEquals("GET", request.method)
      assertEquals("application/json", request.headers["Content-Type"])
      assertEquals("Bearer $mockJwtToken", request.headers["Authorization"])
      MatcherAssert.assertThat(request.path, CoreMatchers.endsWith(endpoint))
    }

    val builder = GetRateRequest.builder()

    // getPrices parameters
    var getRateRequest =
      builder
        .type(INDICATIVE_PRICES)
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .countryCode("USA")
        .build()
    testGetRate(
      """/rate
        ?type=indicative_prices
        &sell_asset=iso4217%3AUSD
        &sell_amount=100
        &sell_delivery_method=WIRE
        &country_code=USA""".replace(
        "\n        ",
        ""
      ),
      getRateRequest
    )

    // getPrice parameters
    getRateRequest =
      builder
        .type(INDICATIVE_PRICE)
        .context(SEP31)
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .buyAsset("stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN")
        .buyDeliveryMethod("CASH")
        .countryCode("USA")
        .build()
    testGetRate(
      """/rate
        ?type=indicative_price
        &context=sep31
        &sell_asset=iso4217%3AUSD
        &sell_amount=100
        &sell_delivery_method=WIRE
        &buy_asset=stellar%3AUSDC%3AGA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN
        &buy_delivery_method=CASH
        &country_code=USA""".replace(
        "\n        ",
        ""
      ),
      getRateRequest
    )

    // postQuote parameters
    getRateRequest =
      builder
        .type(FIRM)
        .context(SEP31)
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .buyAsset("stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN")
        .buyDeliveryMethod("CASH")
        .countryCode("USA")
        .build()
    testGetRate(
      """/rate
        ?type=firm
        &context=sep31
        &sell_asset=iso4217%3AUSD
        &sell_amount=100
        &sell_delivery_method=WIRE
        &buy_asset=stellar%3AUSDC%3AGA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN
        &buy_delivery_method=CASH
        &country_code=USA""".replace(
        "\n        ",
        ""
      ),
      getRateRequest
    )

    // all parameters
    getRateRequest =
      builder
        .type(FIRM)
        .context(SEP31)
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .buyAsset("stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN")
        .buyAmount("100")
        .buyDeliveryMethod("WIRE")
        .countryCode("USA")
        .expireAfter("2022-04-30T02:15:44.000Z")
        .clientId("GDGWTSQKQQAT2OXRSFLADMN4F6WJQMPJ5MIOKIZ2AMBYUI67MJA4WRLA")
        .build()
    testGetRate(
      """/rate
        ?type=firm
        &context=sep31
        &sell_asset=iso4217%3AUSD
        &sell_amount=100
        &sell_delivery_method=WIRE
        &buy_asset=stellar%3AUSDC%3AGA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN
        &buy_amount=100
        &buy_delivery_method=WIRE
        &country_code=USA
        &expire_after=2022-04-30T02%3A15%3A44.000Z
        &client_id=GDGWTSQKQQAT2OXRSFLADMN4F6WJQMPJ5MIOKIZ2AMBYUI67MJA4WRLA""".replace(
        "\n        ",
        ""
      ),
      getRateRequest
    )
  }

  @Test
  fun test_getRate_errorHandling() {
    val validateRequest =
        {
      statusCode: Int,
      responseBody: String?,
      wantException: AnchorException,
      type: GetRateRequest.Type ->
      // mock response
      var mockResponse =
        MockResponse().addHeader("Content-Type", "application/json").setResponseCode(statusCode)
      if (responseBody != null) mockResponse = mockResponse.setBody(responseBody)
      server.enqueue(mockResponse)

      // execute command
      val dummyRequest = GetRateRequest.builder().type(type).build()
      val ex = assertThrows<AnchorException> { rateIntegration.getRate(dummyRequest) }

      // validate exception
      assertEquals(wantException.javaClass, ex.javaClass)
      assertEquals(wantException.message, ex.message)

      // validateRequest
      val request = server.takeRequest()
      assertEquals("GET", request.method)
      assertEquals("application/json", request.headers["Content-Type"])
      assertEquals("Bearer $mockJwtToken", request.headers["Authorization"])
      MatcherAssert.assertThat(request.path, CoreMatchers.endsWith("/rate?type=$type"))
      assertEquals("", request.body.readUtf8())
    }

    // 400 without body
    validateRequest(400, null, BadRequestException("Bad Request"), INDICATIVE_PRICES)

    // 400 with body
    validateRequest(
      400,
      """{"error": "foo 400"}""",
      BadRequestException("foo 400"),
      INDICATIVE_PRICES
    )

    // 404 without body
    validateRequest(404, null, NotFoundException("Not Found"), INDICATIVE_PRICES)

    // 404 with body
    validateRequest(
      404,
      """{"error": "foo 404"}""",
      NotFoundException("foo 404"),
      INDICATIVE_PRICES
    )

    // 422 without body
    validateRequest(422, null, BadRequestException("Bad Request"), INDICATIVE_PRICES)

    // 422 with body
    validateRequest(
      422,
      """{"error": "foo 422"}""",
      BadRequestException("foo 422"),
      INDICATIVE_PRICES
    )

    // 500
    validateRequest(
      500,
      """{"error": "foo 500"}""",
      ServerErrorException("internal server error"),
      INDICATIVE_PRICES
    )

    // 200 with invalid body
    val serverErrorException = ServerErrorException("internal server error")
    validateRequest(
      200,
      """{"rate": {"price": "invalid json",}}""",
      serverErrorException,
      INDICATIVE_PRICES
    )

    // 200 where getRateResponse is missing "price"
    validateRequest(200, """{"rate": "missing price"}""", serverErrorException, INDICATIVE_PRICES)

    // 200 for type=firm|indicative_price where getRateResponse is missing "fee" and "total_price"
    validateRequest(200, """{"rate": {"price": "1"} }""", serverErrorException, FIRM)
    validateRequest(200, """{"rate": {"price": "1"} }""", serverErrorException, INDICATIVE_PRICE)

    // 200 for type=firm|indicative_price where getRateResponse is missing "fee"
    validateRequest(
      200,
      """{"rate": {"price": "1", "total_price": "1.01"} }""",
      serverErrorException,
      FIRM
    )
    validateRequest(
      200,
      """{"rate": {"price": "1", "total_price": "1.01"} }""",
      serverErrorException,
      INDICATIVE_PRICE
    )

    // 200 for type=firm|indicative_price where getRateResponse is missing "total_price"
    var body =
      """{
      "rate": {
        "price": "1",
        "fee": {
          "total": "1.00",
          "asset": "iso4217:USD"
        }
      }
    }""".trimMargin()
    validateRequest(200, body, serverErrorException, FIRM)
    validateRequest(200, body, serverErrorException, INDICATIVE_PRICE)

    // 200 for type=firm where getRateResponse is missing "id"
    body =
      """{
      "rate": {
        "price": "1",
        "total_price": "1.01",
        "fee": {
          "total": "1.00",
          "asset": "iso4217:USD"
        }
      }
    }""".trimMargin()
    validateRequest(200, body, serverErrorException, FIRM)

    // 200 for type=firm where getRateResponse is missing "id" but contains "expires_at"
    body =
      """{
      "rate": {
        "price": "1",
        "total_price": "1.01",
        "expires_at": "2022-04-30T02:15:44.000Z",
        "fee": {
          "total": "1.00",
          "asset": "iso4217:USD"
        }
      }
    }""".trimMargin()
    validateRequest(200, body, serverErrorException, FIRM)

    // 200 for type=firm where getRateResponse is missing "expires_at"
    body =
      """{
      "rate": {
        "id": "my-id",
        "price": "1",
        "total_price": "1.01",
        "fee": {
          "total": "1.00",
          "asset": "iso4217:USD"
        }
      }
    }""".trimMargin()
    validateRequest(200, body, serverErrorException, FIRM)

    // 200 for type=firm where getRateResponse's "expires_at" is invalid
    body =
      """{
      "rate": {
        "id": "my-id",
        "price": "1",
        "total_price": "1.01",
        "expires_at": "foo bar",
        "fee": {
          "total": "1.00",
          "asset": "iso4217:USD"
        }
      }
    }""".trimMargin()
    validateRequest(200, body, serverErrorException, FIRM)
  }

  @Test
  fun test_getRate_jsonBody() {
    val fee = mockSellAssetFee("iso4217:USD")

    val validateRequest =
        { type: GetRateRequest.Type, responseBody: String, wantResponse: GetRateResponse ->
      // mock response
      val mockResponse =
        MockResponse()
          .addHeader("Content-Type", "application/json")
          .setResponseCode(200)
          .setBody(responseBody)
      server.enqueue(mockResponse)

      // execute command
      val dummyRequest = GetRateRequest.builder().type(type).build()
      var gotResponse: GetRateResponse? = null
      assertDoesNotThrow { gotResponse = rateIntegration.getRate(dummyRequest) }
      assertEquals(wantResponse, gotResponse)

      // validateRequest
      val request = server.takeRequest()
      assertEquals("GET", request.method)
      assertEquals("application/json", request.headers["Content-Type"])
      assertEquals("Bearer $mockJwtToken", request.headers["Authorization"])
      MatcherAssert.assertThat(request.path, CoreMatchers.endsWith("/rate?type=$type"))
      assertEquals("", request.body.readUtf8())
    }

    // indicative_prices quote successful response
    var wantGetRateResponse = GetRateResponse.indicativePrices("1.02", "102", "100")
    validateRequest(
      INDICATIVE_PRICES,
      """{
        "rate": {
          "price": "1.02",
          "sell_amount": "102",
          "buy_amount": "100"
        }
      }""".trimMargin(),
      wantGetRateResponse
    )

    // indicative_price quote successful response
    wantGetRateResponse = GetRateResponse.indicativePrice("1.02", "1.04", "104", "100", fee)
    validateRequest(
      INDICATIVE_PRICE,
      """{
        "rate": {
          "price": "1.02",
          "total_price": "1.04",
          "sell_amount": "104",
          "buy_amount": "100",
          "fee": {
            "total": "2.00",
            "asset": "iso4217:USD",
            "details": [
              {
                "name": "Sell fee",
                "amount": "2.00"
              }
            ]
          }
        }
      }""".trimMargin(),
      wantGetRateResponse
    )

    // firm quote
    val instantNow = DateTimeFormatter.ISO_INSTANT.parse("2022-04-30T02:15:44.000Z", Instant::from)
    mockkStatic(Instant::class)
    every { Instant.now() } returns instantNow

    val quote = Quote()
    quote.id = "my-id"
    quote.totalPrice = "1.04"
    quote.price = "1.02"
    quote.sellAmount = "104"
    quote.buyAmount = "100"
    quote.expiresAt = Instant.now()
    quote.fee = fee

    wantGetRateResponse = quote.toGetRateResponse()
    validateRequest(
      FIRM,
      """{
        "rate": {
          "price": "1.02",
          "total_price": "1.04",
          "sell_amount": "104",
          "buy_amount": "100",
          "id": "my-id",
          "expires_at": "2022-04-30T02:15:44.000Z",
          "fee": {
            "total": "2.00",
            "asset": "iso4217:USD",
            "details": [
              {
                "name": "Sell fee",
                "amount": "2.00"
              }
            ]
          }
        }
      }""".trimMargin(),
      wantGetRateResponse
    )
    verify(atLeast = 1) { Instant.now() }
  }
}
