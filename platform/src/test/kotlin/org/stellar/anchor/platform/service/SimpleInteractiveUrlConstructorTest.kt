package org.stellar.anchor.platform.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.time.Instant
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.web.util.UriComponentsBuilder
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.Sep24InteractiveUrlJwt
import org.stellar.anchor.config.CustodySecretConfig
import org.stellar.anchor.config.SecretConfig
import org.stellar.anchor.platform.config.PropertySep24Config
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.util.GsonUtils

@Suppress("UNCHECKED_CAST")
class SimpleInteractiveUrlConstructorTest {
  companion object {
    private val gson = GsonUtils.getInstance()
    @JvmStatic
    private fun constructorTestValues() =
      Stream.of(Arguments.of(CONFIG_JSON_1, REQUEST_JSON_1, TXN_JSON_1))
  }

  @MockK(relaxed = true) private lateinit var secretConfig: SecretConfig
  @MockK(relaxed = true) private lateinit var custodySecretConfig: CustodySecretConfig
  private lateinit var jwtService: JwtService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { secretConfig.sep24InteractiveUrlJwtSecret } returns "sep24_jwt_secret"

    jwtService = JwtService(secretConfig, custodySecretConfig)
  }

  @ParameterizedTest
  @MethodSource("constructorTestValues")
  fun `test correct constructor`(configJson: String, requestJson: String, txnJson: String) {
    val config = gson.fromJson(configJson, PropertySep24Config.InteractiveUrlConfig::class.java)
    val request = gson.fromJson(requestJson, HashMap::class.java)
    val txn = gson.fromJson(txnJson, JdbcSep24Transaction::class.java)

    val constructor = SimpleInteractiveUrlConstructor(config, jwtService)

    var jwt = parseJwtFromUrl(constructor.construct(txn, request as HashMap<String, String>?))
    testJwt(jwt)
    assertEquals("GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO:1234", jwt.sub)

    txn.sep10AccountMemo = null
    jwt = parseJwtFromUrl(constructor.construct(txn, request))
    testJwt(jwt)
    assertEquals("GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO", jwt.sub)
  }

  private fun parseJwtFromUrl(url: String?): Sep24InteractiveUrlJwt {
    val params = UriComponentsBuilder.fromUriString(url!!).build().queryParams
    val cipher = params["token"]!![0]
    return jwtService.decode(cipher, Sep24InteractiveUrlJwt::class.java)
  }

  private fun testJwt(jwt: Sep24InteractiveUrlJwt) {
    val claims = jwt.claims()
    assertEquals("txn_123", jwt.jti as String)
    assertTrue(Instant.ofEpochSecond(jwt.exp).isAfter(Instant.now()))
    val data = claims["data"] as Map<String, String>
    assertEquals("deposit", data["kind"] as String)
    assertEquals("100", data["amount"] as String)
    assertEquals("en", data["lang"] as String)
    assertEquals(
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
      data["amount_in_asset"] as String
    )
    assertEquals("en", data["lang"] as String)
    assertNull(data["email_address"])

    // Name is in request but not in transaction. It must not be included.
    assertNull(data["name"])
  }
}

private const val CONFIG_JSON_1 =
  """
{
  "baseUrl": "http://localhost:8080/sep24/interactive",
  "jwtExpiration": 600,
  "txnFields": [
    "kind",
    "amount_in",
    "amount_in_asset",
    "asset_code"
  ]
}
"""

private const val REQUEST_JSON_1 =
  """
{
  "name": "John Doe",
  "first_name": "John",
  "last_name": "Doe",
  "email_address": "john_doe@stellar.org",
  "lang": "en",
  "amount": "100"
}
"""

private const val TXN_JSON_1 =
  """
{
  "id": "123",
  "transaction_id": "txn_123",
  "status": "incomplete",
  "kind" : "deposit",
  "sep10_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
  "sep10_account_memo": "1234",
  "amount_in": "100",
  "amount_in_asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
}  
"""
