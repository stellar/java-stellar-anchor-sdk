package org.stellar.anchor.platform

import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.ANCHOR_TO_PLATFORM_SECRET
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.PLATFORM_TO_ANCHOR_SECRET
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.OkHttpUtil

const val GET_TRANSACTIONS_ENDPOINT = "/transactions"
const val GET_TRANSACTIONS_MY_ID_ENDPOINT = "/transactions/my_id"
const val PATCH_TRANSACTIONS_ENDPOINT = "/transactions"
const val GET_EXCHANGE_QUOTES_ENDPOINT = "/exchange/quotes"
const val GET_EXCHANGE_QUOTES_ID_ENDPOINT = "/exchange/quotes/id"

open class AbstractAuthIntegrationTest {
  companion object {
    private val jwtService =
      JwtService(null, null, null, PLATFORM_TO_ANCHOR_SECRET, ANCHOR_TO_PLATFORM_SECRET)
    internal val jwtAuthHelper = AuthHelper.forJwtToken(jwtService, 10000)
    internal val apiAuthHelper = AuthHelper.forApiKey(ANCHOR_TO_PLATFORM_SECRET)
    internal val nonAuthHelper = AuthHelper.forNone()
    internal lateinit var testProfileRunner: TestProfileExecutor
  }

  init {}
}

internal class JwtAuthIntegrationTest : AbstractAuthIntegrationTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      testProfileRunner =
        TestProfileExecutor(
          TestConfig(profileName = "default").also {
            // enable platform server jwt auth
            it.env["platform_server.auth.type"] = "jwt"
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

  private val jwtPlatformClient: PlatformApiClient =
    PlatformApiClient(jwtAuthHelper, "http://localhost:8085")

  // TODO - to be deprecated by platformAPI client
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.MINUTES)
      .readTimeout(10, TimeUnit.MINUTES)
      .writeTimeout(10, TimeUnit.MINUTES)
      .build()

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
    val httpRequest =
      Request.Builder()
        .url("http://localhost:8085/transactions")
        .header("Content-Type", "application/json")
        .method(method, getDummyRequestBody(method))
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertEquals(403, response.code)
  }

  private fun getDummyRequestBody(method: String): RequestBody? {
    return if (method != "PATCH") null
    else OkHttpUtil.buildJsonRequestBody(gson.toJson(mapOf("proposedAssetsJson" to "bar")))
  }
}

internal class ApiKeyAuthIntegrationTest : AbstractAuthIntegrationTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      testProfileRunner =
        TestProfileExecutor(
          TestConfig(profileName = "default").also {
            // enable platform server api_key auth
            it.env["platform_server.auth.type"] = "api_key"
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

  private val gson = GsonUtils.getInstance()
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.MINUTES)
      .readTimeout(10, TimeUnit.MINUTES)
      .writeTimeout(10, TimeUnit.MINUTES)
      .build()

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
  fun `test API_KEY auth protection of the platform server`(method: String, endpoint: String) {
    val httpRequest =
      Request.Builder()
        .url("http://localhost:${AbstractIntegrationTest.PLATFORM_SERVER_PORT}$endpoint")
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
        GET_TRANSACTIONS_ENDPOINT,
        PATCH_TRANSACTIONS_ENDPOINT,
        GET_TRANSACTIONS_MY_ID_ENDPOINT,
        GET_EXCHANGE_QUOTES_ENDPOINT,
        GET_EXCHANGE_QUOTES_ID_ENDPOINT
      ]
  )
  fun `test the platform endpoints with API_KEY auth`(method: String, endpoint: String) {
    val httpRequest =
      Request.Builder()
        .url("http://localhost:${AbstractIntegrationTest.PLATFORM_SERVER_PORT}$endpoint")
        .header("Content-Type", "application/json")
        .header("X-Api-Key", ANCHOR_TO_PLATFORM_SECRET)
        .method(method, getDummyRequestBody(method))
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    Assertions.assertNotEquals(403, response.code)
  }

  private fun getDummyRequestBody(method: String): RequestBody? {
    return if (method != "PATCH") null
    else OkHttpUtil.buildJsonRequestBody(gson.toJson(mapOf("proposedAssetsJson" to "bar")))
  }
}
