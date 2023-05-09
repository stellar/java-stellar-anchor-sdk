package org.stellar.anchor.platform.custody.fireblocks

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
import org.stellar.anchor.api.exception.FireblocksException
import org.stellar.anchor.api.exception.InvalidConfigException
import org.stellar.anchor.platform.config.CustodySecretConfig
import org.stellar.anchor.platform.config.FireblocksConfig
import org.stellar.anchor.util.FileUtil.getResourceFileAsString

class FireblocksApiClientTest {

  companion object {
    private const val API_KEY = "testApiKey"
    private const val BASE_URL = "http://testBaseUrl.com"
  }

  @MockK(relaxed = true) private lateinit var client: OkHttpClient

  @MockK(relaxed = true) private lateinit var secretConfig: CustodySecretConfig

  private lateinit var fireblocksApiClient: FireblocksApiClient

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    every { secretConfig.fireblocksApiKey } returns API_KEY
    every { secretConfig.fireblocksSecretKey } returns
      getResourceFileAsString("custody/fireblocks/client/secret_key.txt")

    val fireblocksConfig = FireblocksConfig(secretConfig)
    fireblocksConfig.baseUrl = BASE_URL

    fireblocksApiClient = FireblocksApiClient(client, fireblocksConfig)
  }

  @Test
  fun test_init_invalidSecretKey() {
    every { secretConfig.fireblocksSecretKey } returns "dGVzdA=="
    val fireblocksConfig = FireblocksConfig(secretConfig)

    val exception =
      assertThrows<InvalidConfigException> { FireblocksApiClient(client, fireblocksConfig) }

    Assertions.assertEquals("Failed to generate Fireblocks private key", exception.message)
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
    JSONAssert.assertEquals(
      getResourceFileAsString("custody/fireblocks/client/success_response_body.json"),
      result,
      JSONCompareMode.STRICT
    )

    validateToken(
      requestCapture.captured.header("Authorization")!!,
      "/getPath",
      "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"
    )
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
      """
        Fireblocks API returned an error. HTTP status[400], response[{
          "error_code": "12345",
          "message": "Fireblocks error"
        }]
      """
        .trimIndent(),
      exception.message?.trimIndent()
    )
  }

  @Test
  fun test_postRequest_success() {
    val response = getSuccessMockResponse()
    val requestCapture = slot<Request>()
    val call = mockk<Call>()

    every { client.newCall(capture(requestCapture)) } returns call
    every { call.execute() } returns response

    val result =
      fireblocksApiClient.post(
        "/postPath",
        getResourceFileAsString("custody/fireblocks/client/request_body.json")?.trimIndent()
      )

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
    JSONAssert.assertEquals(
      getResourceFileAsString("custody/fireblocks/client/request_body.json"),
      requestBodyToString(requestCapture.captured.body),
      true
    )
    JSONAssert.assertEquals(
      getResourceFileAsString("custody/fireblocks/client/success_response_body.json"),
      result,
      JSONCompareMode.STRICT
    )

    validateToken(
      requestCapture.captured.header("Authorization")!!,
      "/postPath",
      "a2b3902dd7424e90b5dcf6d49438736f41e166e8cfdc0fc185eaaf8d22c83e6b4075273988d377158e5e9066d50b4ecbce2e872515d9b94a9469c09ec6cd119e"
    )
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
      .body(
        getResourceFileAsString("custody/fireblocks/client/success_response_body.json")
          .toResponseBody("application/json".toMediaTypeOrNull())
      )
      .build()
  }

  private fun getErrorMockResponse(): Response {
    return Response.Builder()
      .request(Request.Builder().url("http://test.com").build())
      .protocol(Protocol.HTTP_1_1)
      .code(400)
      .message("ERROR")
      .body(
        getResourceFileAsString("custody/fireblocks/client/error_response_body.json")
          .toResponseBody("application/json".toMediaTypeOrNull())
      )
      .build()
  }

  private fun validateToken(token: String, path: String, bodyHash: String) {
    Assertions.assertNotNull(token)
    Assertions.assertTrue(token.startsWith("Bearer "))

    val claims =
      Jwts.parser()
        .setSigningKey(getPublicKey())
        .parseClaimsJws(StringUtils.substringAfter(token, "Bearer "))
        .body

    Assertions.assertEquals(API_KEY, claims.subject)
    Assertions.assertTrue(claims.issuedAt.toInstant().isBefore(Instant.now()))
    Assertions.assertTrue(claims.expiration.toInstant().isAfter(Instant.now()))
    Assertions.assertTrue(isUuid(claims.get("nonce").toString()))
    Assertions.assertEquals(path, claims.get("uri"))
    Assertions.assertEquals(bodyHash, claims.get("bodyHash"))
  }

  private fun getPublicKey(): PublicKey? {
    try {
      val publicKeyBytes =
        Base64.getDecoder()
          .decode(
            getResourceFileAsString("custody/fireblocks/client/public_key.txt")
              .replace("-----BEGIN PUBLIC KEY-----", StringUtils.EMPTY)
              .replace("-----END PUBLIC KEY-----", StringUtils.EMPTY)
              .replace(StringUtils.LF, StringUtils.EMPTY)
              .replace(StringUtils.CR, StringUtils.EMPTY)
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
