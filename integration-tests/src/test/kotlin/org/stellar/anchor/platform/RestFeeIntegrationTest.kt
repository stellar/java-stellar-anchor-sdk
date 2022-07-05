package org.stellar.anchor.platform

import io.mockk.*
import java.util.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetFeeResponse
import org.stellar.anchor.api.exception.AnchorException
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.exception.ServerErrorException
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.JwtToken
import org.stellar.anchor.platform.callback.RestFeeIntegration
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.OkHttpUtil

class RestFeeIntegrationTest {
  companion object {
    private const val fiatUSD = "iso4217:USD"
    private const val stellarCircleUSDC =
      "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"

    private const val PLATFORM_TO_ANCHOR_SECRET = "myPlatformToAnchorSecret"
    private const val JWT_EXPIRATION_MILLISECONDS: Long = 1000000
    private val platformToAnchorJwtService = JwtService(PLATFORM_TO_ANCHOR_SECRET)
    private val authHelper =
      AuthHelper.forJwtToken(
        platformToAnchorJwtService,
        JWT_EXPIRATION_MILLISECONDS,
        "http://localhost:8080"
      )
  }
  private lateinit var server: MockWebServer
  private lateinit var feeIntegration: RestFeeIntegration
  private lateinit var mockJwtToken: String
  private val gson = GsonUtils.getInstance()

  @BeforeEach
  fun setUp() {
    server = MockWebServer()
    server.start()
    feeIntegration =
      RestFeeIntegration(
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

  private fun mockGetFeeResponse(
    amount: String,
    asset: String,
  ): MockResponse {
    val fee =
      hashMapOf(
        "amount" to amount,
        "asset" to asset,
      )

    val bodyMap = hashMapOf("fee" to fee)
    return MockResponse()
      .addHeader("Content-Type", "application/json")
      .setResponseCode(200)
      .setBody(gson.toJson(bodyMap))
  }

  @Test
  fun test_getFee() {
    val getFeeRequest =
      GetFeeRequest.builder()
        .sendAsset(fiatUSD)
        .sendAmount("10")
        .receiveAsset(stellarCircleUSDC)
        .clientId("<client-id>")
        .senderId("<sender-id>")
        .receiverId("<receiver-id>")
        .build()

    server.enqueue(mockGetFeeResponse("10", fiatUSD))

    val getFeeResponse = feeIntegration.getFee(getFeeRequest)
    assertInstanceOf(GetFeeResponse::class.java, getFeeResponse)

    val request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals("Bearer $mockJwtToken", request.headers["Authorization"])
    MatcherAssert.assertThat(
      request.path,
      CoreMatchers.endsWith(
        """
        /fee
        ?send_asset=iso4217%3AUSD
        &receive_asset=stellar%3AUSDC%3AGBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5
        &send_amount=10
        &client_id=%3Cclient-id%3E
        &sender_id=%3Csender-id%3E
        &receiver_id=%3Creceiver-id%3E""".replace(
          "\n        ",
          ""
        )
      )
    )
  }

  @Test
  fun test_getFee_errorHandling() {
    val validateRequest =
        { statusCode: Int, responseBody: String?, wantException: AnchorException ->
      // mock response
      var mockResponse =
        MockResponse().addHeader("Content-Type", "application/json").setResponseCode(statusCode)
      if (responseBody != null) mockResponse = mockResponse.setBody(responseBody)
      server.enqueue(mockResponse)

      // execute command
      val dummyRequest = GetFeeRequest.builder().build()
      val ex = assertThrows<AnchorException> { feeIntegration.getFee(dummyRequest) }

      // validate exception
      assertEquals(wantException.javaClass, ex.javaClass)
      assertEquals(wantException.message, ex.message)

      // validateRequest
      val request = server.takeRequest()
      assertEquals("GET", request.method)
      assertEquals("application/json", request.headers["Content-Type"])
      assertEquals("Bearer $mockJwtToken", request.headers["Authorization"])
      MatcherAssert.assertThat(request.path, CoreMatchers.endsWith("/fee"))
      assertEquals("", request.body.readUtf8())
    }

    // 400 without body
    validateRequest(400, null, BadRequestException("Bad Request"))

    // 400 with body
    validateRequest(
      400,
      """{"error": "Invalid fee request."}""",
      BadRequestException("Invalid fee request."),
    )

    // 404 without body
    validateRequest(404, null, NotFoundException("Not Found"))

    // 422 without body
    validateRequest(422, null, BadRequestException("Bad Request"))

    // 500 with body
    validateRequest(500, """{"error": "foo 500"}""", ServerErrorException("internal server error"))
  }
}
