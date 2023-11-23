package org.stellar.anchor.platform

import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.exception.*
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.platform.callback.RestCustomerIntegration
import org.stellar.anchor.platform.callback.RestFeeIntegration
import org.stellar.anchor.platform.callback.RestRateIntegration
import org.stellar.anchor.util.OkHttpUtil

internal class PlatformJwtAuthIntegrationTest : AbstractAuthIntegrationTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      println("Running PlatformJwtAuthIntegrationTest")
      testProfileRunner =
        TestProfileExecutor(
          TestConfig().also {
            it.env[RUN_DOCKER] = "true"
            it.env[RUN_ALL_SERVERS] = "false"
            it.env[RUN_PLATFORM_SERVER] = "true"

            // enable platform server jwt auth
            it.env["platform_server.auth.type"] = "JWT"
            // enable business server callback auth
            it.env["auth.type"] = "JWT"
            it.env["auth.platformToAnchorSecret"] =
              AbstractIntegrationTest.PLATFORM_TO_ANCHOR_SECRET
            it.env["auth.anchorToPlatformSecret"] =
              AbstractIntegrationTest.ANCHOR_TO_PLATFORM_SECRET
            it.env["auth.expirationMilliseconds"] =
              AbstractIntegrationTest.JWT_EXPIRATION_MILLISECONDS.toString()
          }
        )
      testProfileRunner.start()
    }

    @AfterAll
    @JvmStatic
    fun breakdown() {
      testProfileRunner.shutdown()
    }
  }

  // TODO - to be deprecated by platformAPI client
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.MINUTES)
      .readTimeout(10, TimeUnit.MINUTES)
      .writeTimeout(10, TimeUnit.MINUTES)
      .build()

  private val jwtPlatformClient: PlatformApiClient =
    PlatformApiClient(jwtAuthHelper, "http://localhost:8085")
  private val jwtWrongKeyPlatformClient: PlatformApiClient =
    PlatformApiClient(jwtWrongKeyAuthHelper, "http://localhost:8085")
  private val jwtExpiredTokenPlatformClient: PlatformApiClient =
    PlatformApiClient(jwtExpiredAuthHelper, "http://localhost:8085")

  @ParameterizedTest
  @CsvSource(
    value =
      [
        GET_TRANSACTIONS_ENDPOINT,
        PATCH_TRANSACTIONS_ENDPOINT,
        GET_TRANSACTIONS_MY_ID_ENDPOINT,
        GET_EXCHANGE_QUOTES_ENDPOINT,
        GET_EXCHANGE_QUOTES_ID_ENDPOINT
      ]
  )
  fun `test the platform endpoints with JWT auth`(method: String, endpoint: String) {
    // Assert the request does not throw a 403.
    // As for the correctness of the request/response, it should be tested in the platform server
    // integration tests.
    assertThrows<SepNotFoundException> { jwtPlatformClient.getTransaction("my_id") }
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        GET_TRANSACTIONS_ENDPOINT,
        PATCH_TRANSACTIONS_ENDPOINT,
        GET_TRANSACTIONS_MY_ID_ENDPOINT,
        GET_EXCHANGE_QUOTES_ENDPOINT,
        GET_EXCHANGE_QUOTES_ID_ENDPOINT
      ]
  )
  fun `test JWT protection of the platform server`(method: String, endpoint: String) {
    // Check if the request without JWT will cause a 403.
    val httpRequest =
      Request.Builder()
        .url("http://localhost:8085/transactions")
        .header("Content-Type", "application/json")
        .method(method, getPlatformDummyRequestBody(method))
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertEquals(403, response.code)

    // Check if the wrong JWT key will cause a 403.
    assertThrows<SepNotAuthorizedException> { jwtWrongKeyPlatformClient.getTransaction("my_id") }
    assertThrows<SepNotAuthorizedException> {
      jwtExpiredTokenPlatformClient.getTransaction("my_id")
    }
  }

  private fun getPlatformDummyRequestBody(method: String): RequestBody? {
    return if (method != "PATCH") null
    else OkHttpUtil.buildJsonRequestBody(gson.toJson(mapOf("proposedAssetsJson" to "bar")))
  }

  @Test
  // This is disabled because it is testing the callback auth instead of platform auth
  @Disabled
  fun `test the callback customer endpoint with JWT auth`() {
    val rci =
      RestCustomerIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtAuthHelper,
        gson
      )
    // Assert the request does not throw a 403.
    assertThrows<NotFoundException> {
      rci.getCustomer(GetCustomerRequest.builder().id("1").build())
    }
  }

  @Test
  // This is disabled because it is testing the callback auth instead of platform auth
  @Disabled
  fun `test the callback rate endpoint with JWT auth`() {
    val rri =
      RestRateIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtAuthHelper,
        gson
      )
    // Assert the request does not throw a 403.
    assertThrows<BadRequestException> { rri.getRate(GetRateRequest.builder().build()) }
  }

  @Test
  // This is disabled because it is testing the callback auth instead of platform auth
  @Disabled
  fun `test the callback fee endpoint with JWT auth`() {
    val rfi =
      RestFeeIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtAuthHelper,
        gson
      )
    // Assert the request does not throw a 403.
    assertThrows<BadRequestException> { rfi.getFee(GetFeeRequest.builder().build()) }
  }

  @Test
  fun `test JWT protection of callback customer endpoint`() {
    val badTokenClient =
      RestCustomerIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtWrongKeyAuthHelper,
        gson
      )
    assertThrows<ServerErrorException> {
      badTokenClient.getCustomer(GetCustomerRequest.builder().id("1").build())
    }

    val expiredTokenClient =
      RestCustomerIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtWrongKeyAuthHelper,
        gson
      )
    assertThrows<ServerErrorException> {
      expiredTokenClient.getCustomer(GetCustomerRequest.builder().id("1").build())
    }
  }

  @Test
  fun `test JWT protection of callback rate endpoint`() {
    val badTokenClient =
      RestRateIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtWrongKeyAuthHelper,
        gson
      )
    assertThrows<ServerErrorException> { badTokenClient.getRate(GetRateRequest.builder().build()) }

    val expiredTokenClient =
      RestRateIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtExpiredAuthHelper,
        gson
      )
    assertThrows<ServerErrorException> {
      expiredTokenClient.getRate(GetRateRequest.builder().build())
    }
  }

  @Test
  fun `test JWT protection of callback fee endpoint with bad token`() {
    val badTokenClient =
      RestFeeIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtWrongKeyAuthHelper,
        gson
      )
    assertThrows<ServerErrorException> { badTokenClient.getFee(GetFeeRequest.builder().build()) }

    val expiredTokenClient =
      RestFeeIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtExpiredAuthHelper,
        gson
      )
    assertThrows<ServerErrorException> {
      expiredTokenClient.getFee(GetFeeRequest.builder().build())
    }
  }
}
