package org.stellar.anchor.platform

import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest
import org.stellar.anchor.api.exception.*
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.ANCHOR_TO_PLATFORM_SECRET
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.CUSTODY_SERVER_SERVER_PORT
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.JWT_EXPIRATION_MILLISECONDS
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.PLATFORM_TO_ANCHOR_SECRET
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.PLATFORM_TO_CUSTODY_SECRET
import org.stellar.anchor.platform.callback.RestCustomerIntegration
import org.stellar.anchor.platform.callback.RestFeeIntegration
import org.stellar.anchor.platform.callback.RestRateIntegration
import org.stellar.anchor.platform.config.CustodyApiConfig
import org.stellar.anchor.platform.config.PropertyCustodySecretConfig
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.OkHttpUtil

const val GET_TRANSACTIONS_ENDPOINT = "GET,/transactions"
const val PATCH_TRANSACTIONS_ENDPOINT = "PATCH,/transactions"
const val GET_TRANSACTIONS_MY_ID_ENDPOINT = "GET,/transactions/my_id"
const val GET_EXCHANGE_QUOTES_ENDPOINT = "GET,/exchange/quotes"
const val GET_EXCHANGE_QUOTES_ID_ENDPOINT = "GET,/exchange/quotes/id"
const val POST_CUSTODY_TRANSACTION_ENDPOINT = "POST,/transactions"

open class AbstractAuthIntegrationTest {
  companion object {
    private val jwtService =
      JwtService(
        null,
        null,
        null,
        PLATFORM_TO_ANCHOR_SECRET,
        ANCHOR_TO_PLATFORM_SECRET,
        PLATFORM_TO_CUSTODY_SECRET
      )
    private val jwtWrongKeyService =
      JwtService(
        null,
        null,
        null,
        PLATFORM_TO_ANCHOR_SECRET + "bad",
        ANCHOR_TO_PLATFORM_SECRET + "bad",
        PLATFORM_TO_CUSTODY_SECRET + "bad"
      )

    internal val jwtAuthHelper = AuthHelper.forJwtToken(jwtService, 10000)
    internal val jwtWrongKeyAuthHelper = AuthHelper.forJwtToken(jwtWrongKeyService, 10000)
    internal val jwtExpiredAuthHelper = AuthHelper.forJwtToken(jwtService, 0)
    internal lateinit var testProfileRunner: TestProfileExecutor
  }
}

internal class PlatformJwtAuthIntegrationTest : AbstractAuthIntegrationTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      testProfileRunner =
        TestProfileExecutor(
          TestConfig(testProfileName = "default").also {
            // enable platform server jwt auth
            it.env["platform_server.auth.type"] = "jwt"
            // enable business server callback auth
            it.env["integration-auth.authType"] = "jwt"
            it.env["integration-auth.platformToAnchorSecret"] = PLATFORM_TO_ANCHOR_SECRET
            it.env["integration-auth.anchorToPlatformSecret"] = ANCHOR_TO_PLATFORM_SECRET
            it.env["integration-auth.expirationMilliseconds"] =
              JWT_EXPIRATION_MILLISECONDS.toString()
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

internal class CustodyJwtAuthIntegrationTest : AbstractAuthIntegrationTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      testProfileRunner =
        TestProfileExecutor(
          TestConfig(testProfileName = "default-custody").also {
            // enable custody server jwt auth
            it.env["custody_server.auth.type"] = "jwt"
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

  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.MINUTES)
      .readTimeout(10, TimeUnit.MINUTES)
      .writeTimeout(10, TimeUnit.MINUTES)
      .build()

  private val custodyApiConfig: CustodyApiConfig =
    CustodyApiConfig(PropertyCustodySecretConfig()).apply { baseUrl = "http://localhost:8086" }

  private val jwtCustodyClient: org.stellar.anchor.platform.apiclient.CustodyApiClient =
    org.stellar.anchor.platform.apiclient.CustodyApiClient(
      httpClient,
      jwtAuthHelper,
      custodyApiConfig
    )
  private val jwtWrongKeyCustodyClient: org.stellar.anchor.platform.apiclient.CustodyApiClient =
    org.stellar.anchor.platform.apiclient.CustodyApiClient(
      httpClient,
      jwtWrongKeyAuthHelper,
      custodyApiConfig
    )
  private val jwtExpiredTokenCustodyClient: org.stellar.anchor.platform.apiclient.CustodyApiClient =
    org.stellar.anchor.platform.apiclient.CustodyApiClient(
      httpClient,
      jwtExpiredAuthHelper,
      custodyApiConfig
    )

  @Test
  fun `test the custody endpoints with JWT auth`() {
    // Assert the request does not throw a 403.
    // As for the correctness of the request/response, it should be tested in the custody server
    // integration tests.
    jwtCustodyClient.createTransaction(getCustodyDummyRequest())
  }

  @ParameterizedTest
  @CsvSource(value = [POST_CUSTODY_TRANSACTION_ENDPOINT])
  fun `test JWT protection of the custody server`(method: String, endpoint: String) {
    // Check if the request without JWT will cause a 403.
    val httpRequest =
      Request.Builder()
        .url("http://localhost:${CUSTODY_SERVER_SERVER_PORT}${endpoint}")
        .header("Content-Type", "application/json")
        .method(method, getCustodyDummyRequestBody())
        .build()
    val response = httpClient.newCall(httpRequest).execute()
    assertEquals(403, response.code)

    // Check if the wrong JWT key will cause a 403.
    assertThrows<CustodyException> {
      jwtWrongKeyCustodyClient.createTransaction(getCustodyDummyRequest())
    }
    assertThrows<CustodyException> {
      jwtExpiredTokenCustodyClient.createTransaction(getCustodyDummyRequest())
    }
  }

  private fun getCustodyDummyRequest(): CreateCustodyTransactionRequest {
    return CreateCustodyTransactionRequest.builder().id("testId").build()
  }

  private fun getCustodyDummyRequestBody(): RequestBody? {
    return OkHttpUtil.buildJsonRequestBody(gson.toJson(mapOf("id" to "testId")))
  }
}

internal class PlatformApiKeyAuthIntegrationTest : AbstractAuthIntegrationTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      testProfileRunner =
        TestProfileExecutor(
          TestConfig(testProfileName = "default").also {
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
        .header("X-Api-Key", ANCHOR_TO_PLATFORM_SECRET)
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

internal class CustodyApiKeyAuthIntegrationTest : AbstractAuthIntegrationTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      testProfileRunner =
        TestProfileExecutor(
          TestConfig(testProfileName = "default-custody").also {
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
        .url("http://localhost:${CUSTODY_SERVER_SERVER_PORT}$endpoint")
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
        .url("http://localhost:${CUSTODY_SERVER_SERVER_PORT}$endpoint")
        .header("Content-Type", "application/json")
        .header("X-Api-Key", PLATFORM_TO_CUSTODY_SECRET)
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
