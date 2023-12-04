package org.stellar.anchor.platform

import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.OkHttpUtil

internal class PlatformApiKeyAuthIntegrationTest : AbstractAuthIntegrationTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      println("Running PlatformApiKeyAuthIntegrationTest")
      testProfileRunner =
        TestProfileExecutor(
          TestConfig().also {
            it.env[RUN_DOCKER] = "true"
            it.env[RUN_ALL_SERVERS] = "false"
            it.env[RUN_PLATFORM_SERVER] = "true"

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
        .method(method, getPlatformDummyRequestBody(method))
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
        .header("X-Api-Key", AbstractIntegrationTest.ANCHOR_TO_PLATFORM_SECRET)
        .method(method, getPlatformDummyRequestBody(method))
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    Assertions.assertNotEquals(403, response.code)
  }

  private fun getPlatformDummyRequestBody(method: String): RequestBody? {
    return if (method != "PATCH") null
    else OkHttpUtil.buildJsonRequestBody(gson.toJson(mapOf("proposedAssetsJson" to "bar")))
  }
}
