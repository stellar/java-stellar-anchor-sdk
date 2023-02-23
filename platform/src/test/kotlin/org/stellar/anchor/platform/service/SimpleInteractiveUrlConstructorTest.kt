package org.stellar.anchor.platform.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.util.UriComponentsBuilder
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.Sep24InteractiveUrlJwt
import org.stellar.anchor.config.SecretConfig
import org.stellar.anchor.platform.config.PropertySep24Config
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.util.GsonUtils

@Suppress("UNCHECKED_CAST")
class SimpleInteractiveUrlConstructorTest {
  companion object {
    private val gson = GsonUtils.getInstance()
  }

  @MockK(relaxed = true) private lateinit var secretConfig: SecretConfig
  private lateinit var jwtService: JwtService
  private lateinit var sep9Fields: HashMap<*, *>
  private lateinit var txn: JdbcSep24Transaction

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { secretConfig.sep24InteractiveUrlJwtSecret } returns "sep24_jwt_secret"

    jwtService = JwtService(secretConfig)
    sep9Fields = gson.fromJson(SEP9_FIELDS_JSON, HashMap::class.java)
    txn = gson.fromJson(TXN_JSON, JdbcSep24Transaction::class.java)
  }

  @Test
  fun `test correct config`() {
    val config =
      gson.fromJson(SIMPLE_CONFIG_JSON, PropertySep24Config.InteractiveUrlConfig::class.java)
    val constructor = SimpleInteractiveUrlConstructor(config, jwtService)

    var jwt =
      parseJwtFromUrl(constructor.construct(txn, "en", sep9Fields as HashMap<String, String>?))
    testJwt(jwt)
    assertEquals("GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO:1234", jwt.sub)

    txn.sep10AccountMemo = null
    jwt = parseJwtFromUrl(constructor.construct(txn, "en", sep9Fields as HashMap<String, String>?))
    testJwt(jwt)
    assertEquals("GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO", jwt.sub)
  }

  private fun testJwt(jwt: Sep24InteractiveUrlJwt) {
    val claims = jwt.claims()
    assertEquals("txn_123", jwt.jti as String)
    assertTrue(Instant.ofEpochSecond(jwt.exp).isAfter(Instant.now()))
    val data = claims["data"] as Map<String, String>
    assertEquals("deposit", data["kind"] as String)
    assertEquals("John Doe", data["name"] as String)
    assertEquals(
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
      data["amount_in_asset"] as String
    )
    assertEquals("en", data["lang"] as String)
    assertEquals("john_doe@stellar.org", data["email"] as String)
  }

  private fun parseJwtFromUrl(url: String?): Sep24InteractiveUrlJwt {
    val params = UriComponentsBuilder.fromUriString(url!!).build().queryParams
    val cipher = params["token"]!![0]
    return jwtService.decode(cipher, Sep24InteractiveUrlJwt::class.java)
  }
}

private const val SIMPLE_CONFIG_JSON =
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

private const val SEP9_FIELDS_JSON =
  """
{
  "name": "John Doe",
  "email": "john_doe@stellar.org"
}
"""

private const val TXN_JSON =
  """
{
  "id": "123",
  "transaction_id": "txn_123",
  "status": "incomplete",
  "kind" : "deposit",
  "sep10account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
  "sep10account_memo": "1234",
  "amount_in": "100",
  "amount_in_asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
}  
"""
