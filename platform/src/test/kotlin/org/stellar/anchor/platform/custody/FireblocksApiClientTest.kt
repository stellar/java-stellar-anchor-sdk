import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.platform.config.FireblocksConfig
import org.stellar.anchor.platform.config.PropertySecretConfig
import org.stellar.anchor.platform.custody.fireblocks.FireblocksApiClient
import org.stellar.anchor.platform.exception.FireblocksException

class FireblocksApiClientTest {

  companion object {
    private const val API_KEY = "testApiKey"
    private const val SECRET_KEY =
      """-----BEGIN PRIVATE KEY-----
MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAhdi6VMkULbC/4HVl
ied/JLjj6H+saFyjIlm0ca7QN1k9+OZh5vP6lt5+ggSi9JDyDvPAh9WpA206M02u
Erh4FwIDAQABAkB2he6qec0mkKe46fxaW+bY6+jVz4kqeS30kx8YtEapW0w56JNe
txrwVmx+eT8Ve8sIwGg3G6GLNyfXsc6AZQvxAiEA5KYpwFp9rpU0VUC9f3oTFrD6
EzP0+T0UHZE1cj9E+10CIQCV23aMiWzSuiuwLBZsVKzFsCHQlYsoxT4BNZkg2J0+
AwIgEA5C/EjebnX3uMzVAbCWyo8e4F5To3TQhsr9j8o1k9kCIDlZGyz9CmA6Tq3E
sXATl2qv1MD1+aNImEnuMQOY4dPxAiEAsAyXwuYF8hObeRWhi+9xDjTpnyAdOwWI
HSi2wrZnRHc=
-----END PRIVATE KEY-----"""
    private const val PUBLIC_KEY =
      """
-----BEGIN PUBLIC KEY-----
MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAIXYulTJFC2wv+B1ZYnnfyS44+h/rGhc
oyJZtHGu0DdZPfjmYebz+pbefoIEovSQ8g7zwIfVqQNtOjNNrhK4eBcCAwEAAQ==
-----END PUBLIC KEY-----
"""
    private const val BASE_URL = "http://testBaseUrl.com"
    private const val SUCCESS_RESPONSE_BODY =
      """{
            "key1": "value1",
            "key2": "value2"
        }"""
    private const val ERROR_RESPONSE_BODY =
      """{
            "error_code": "12345",
            "message": "Fireblocks error"
        }"""
    private const val REQUEST_BODY =
      """{
            "key3": "value3",
            "key4": "value4"
        }"""
  }

  @MockK(relaxed = true) private lateinit var client: OkHttpClient

  @MockK(relaxed = true) private lateinit var secretConfig: PropertySecretConfig

  private lateinit var fireblocksApiClient: FireblocksApiClient

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    every { secretConfig.fireblocksApiKey } returns API_KEY
    every { secretConfig.fireblocksSecretKey } returns SECRET_KEY

    val fireblocksConfig = FireblocksConfig(secretConfig)
    fireblocksConfig.baseUrl = BASE_URL

    fireblocksApiClient = FireblocksApiClient(client, fireblocksConfig)
  }

  @Test
  fun test_init_invalidSecretKey() {
    every { secretConfig.fireblocksSecretKey } returns "dGVzdA=="
    val fireblocksConfig = FireblocksConfig(secretConfig)

    val exception = assertThrows<RuntimeException> { FireblocksApiClient(client, fireblocksConfig) }

    Assertions.assertEquals("Invalid Fireblocks secret key", exception.message)
  }

  @Test
  fun test_getRequest_success() {
    val response = getSuccessMockResponse()
    val requestCapture = slot<Request>()
    val call = mockk<Call>()

    every { client.newCall(capture(requestCapture)) } returns call
    every { call.execute() } returns response

    val result = fireblocksApiClient.get("/getPath")

    Assertions.assertEquals(
      "http://testbaseurl.com/getPath",
      requestCapture.captured.url.toString()
    )
    Assertions.assertEquals("testApiKey", requestCapture.captured.header("X-API-Key"))
    Assertions.assertTrue(requestCapture.captured.header("Authorization")!!.startsWith("Bearer "))
    JSONAssert.assertEquals(SUCCESS_RESPONSE_BODY, result, JSONCompareMode.STRICT)

    validateToken(requestCapture.captured.header("Authorization")!!, "/getPath")
  }

  @Test
  fun test_getRequest_fail_IOException() {
    val call = mockk<Call>()

    every { client.newCall(any()) } returns call
    every { call.execute() } throws IOException("Fireblocks IO exception")

    val exception = assertThrows<FireblocksException> { fireblocksApiClient.get("/getPath") }

    Assertions.assertEquals(
      "Exception occurred during request to Fireblocks API",
      exception.message
    )
  }

  @Test
  fun test_getRequest_fail_errorStatusCode() {
    val response = getErrorMockResponse()
    val requestCapture = slot<Request>()
    val call = mockk<Call>()

    every { client.newCall(capture(requestCapture)) } returns call
    every { call.execute() } returns response

    val exception = assertThrows<FireblocksException> { fireblocksApiClient.get("/getPath") }

    Assertions.assertEquals(
      """Fireblocks API returned an error. HTTP status[400], response[{
            "error_code": "12345",
            "message": "Fireblocks error"
        }]""",
      exception.message
    )
  }

  @Test
  fun test_postRequest_success() {
    val response = getSuccessMockResponse()
    val requestCapture = slot<Request>()
    val call = mockk<Call>()

    every { client.newCall(capture(requestCapture)) } returns call
    every { call.execute() } returns response

    val result = fireblocksApiClient.post("/postPath", REQUEST_BODY)

    Assertions.assertEquals(
      "http://testbaseurl.com/postPath",
      requestCapture.captured.url.toString()
    )
    Assertions.assertEquals("testApiKey", requestCapture.captured.header("X-API-Key"))
    Assertions.assertTrue(requestCapture.captured.header("Authorization")!!.startsWith("Bearer "))
    Assertions.assertEquals(
      "application/json; charset=utf-8",
      requestCapture.captured.body!!.contentType().toString()
    )
    JSONAssert.assertEquals(REQUEST_BODY, requestBodyToString(requestCapture.captured.body), true)
    JSONAssert.assertEquals(SUCCESS_RESPONSE_BODY, result, JSONCompareMode.STRICT)

    validateToken(requestCapture.captured.header("Authorization")!!, "/postPath")
  }

  private fun requestBodyToString(requestBody: RequestBody?): String {
    val buffer = Buffer()
    requestBody?.writeTo(buffer)
    return buffer.readString(Charset.forName("UTF-8"))
  }

  private fun getSuccessMockResponse(): Response {
    return Response.Builder()
      .request(Request.Builder().url("http://test.com").build())
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .body(SUCCESS_RESPONSE_BODY.toResponseBody("application/json".toMediaTypeOrNull()))
      .build()
  }

  private fun getErrorMockResponse(): Response {
    return Response.Builder()
      .request(Request.Builder().url("http://test.com").build())
      .protocol(Protocol.HTTP_1_1)
      .code(400)
      .message("ERROR")
      .body(ERROR_RESPONSE_BODY.toResponseBody("application/json".toMediaTypeOrNull()))
      .build()
  }

  private fun validateToken(token: String, path: String) {
    Assertions.assertNotNull(token)
    Assertions.assertTrue(token.startsWith("Bearer "))

    val claims =
      Jwts.parser()
        .setSigningKey(getPublicKey())
        .parse(StringUtils.substringAfter(token, "Bearer "))
        .body as Claims

    Assertions.assertEquals(API_KEY, claims.subject)
    Assertions.assertTrue(claims.issuedAt.toInstant().isBefore(Instant.now()))
    Assertions.assertTrue(claims.expiration.toInstant().isAfter(Instant.now()))
    Assertions.assertTrue(isUuid(claims.get("nonce").toString()))
    Assertions.assertEquals(path, claims.get("uri"))
    Assertions.assertNotNull(claims.get("bodyHash"))
  }

  private fun getPublicKey(): PublicKey? {
    try {
      val publicKeyBytes =
        Base64.getDecoder()
          .decode(
            PUBLIC_KEY.replace("-----BEGIN PUBLIC KEY-----", StringUtils.EMPTY)
              .replace("-----END PUBLIC KEY-----", StringUtils.EMPTY)
              .replace(StringUtils.LF, StringUtils.EMPTY)
          )
      val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
      val keyFactory = KeyFactory.getInstance("RSA")
      return keyFactory.generatePublic(publicKeySpec)
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return null
  }

  private fun isUuid(text: String): Boolean {
    return try {
      UUID.fromString(text)
      true
    } catch (e: IllegalArgumentException) {
      e.printStackTrace()
      false
    }
  }
}
