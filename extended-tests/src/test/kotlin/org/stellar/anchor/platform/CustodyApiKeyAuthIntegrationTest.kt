package org.stellar.anchor.platform

import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.OkHttpUtil

internal class CustodyApiKeyAuthIntegrationTest : AbstractAuthIntegrationTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      println("Running CustodyApiKeyAuthIntegrationTest")
      testProfileRunner =
        TestProfileExecutor(
          TestConfig(testProfileName = "default-custody").also {
            it.env[RUN_DOCKER] = "true"
            it.env[RUN_ALL_SERVERS] = "false"
            it.env[RUN_CUSTODY_SERVER] = "true"

            // enable custody server api_key auth
            it.env["custody_server.auth.type"] = "api_key"
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
  @CsvSource(value = [POST_CUSTODY_TRANSACTION_ENDPOINT])
  fun test_incomingCustodyAuth_emptyApiKey_authFails(method: String, endpoint: String) {
    val httpRequest =
      Request.Builder()
        .url("http://localhost:${AbstractIntegrationTest.CUSTODY_SERVER_SERVER_PORT}$endpoint")
        .header("Content-Type", "application/json")
        .method(method, getCustodyDummyRequestBody())
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertEquals(403, response.code)
  }

  @ParameterizedTest
  @CsvSource(value = [POST_CUSTODY_TRANSACTION_ENDPOINT])
  fun test_incomingCustodyAuth_emptyApiKey_authPasses(method: String, endpoint: String) {
    val httpRequest =
      Request.Builder()
        .url("http://localhost:${AbstractIntegrationTest.CUSTODY_SERVER_SERVER_PORT}$endpoint")
        .header("Content-Type", "application/json")
        .header("X-Api-Key", AbstractIntegrationTest.PLATFORM_TO_CUSTODY_SECRET)
        .method(method, getCustodyDummyRequestBody())
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertEquals(200, response.code)
  }

  private fun getPlatformDummyRequestBody(method: String): RequestBody? {
    return if (method != "PATCH") null
    else OkHttpUtil.buildJsonRequestBody(gson.toJson(mapOf("proposedAssetsJson" to "bar")))
  }

  private fun getCustodyDummyRequestBody(): RequestBody? {
    return OkHttpUtil.buildJsonRequestBody(gson.toJson(mapOf("id" to "testId")))
  }
}
