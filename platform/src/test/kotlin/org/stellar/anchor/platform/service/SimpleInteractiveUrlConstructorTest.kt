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
import org.stellar.anchor.auth.Sep10Jwt
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
  private lateinit var sep10Jwt: Sep10Jwt
  private lateinit var sep9Fields: HashMap<*, *>
  private lateinit var txn: JdbcSep24Transaction

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    every { secretConfig.getSep10JwtSecretKey() } returns "sep10_jwt_secret"

    jwtService = JwtService(secretConfig)
    sep10Jwt = Sep10Jwt()
    sep9Fields = gson.fromJson(SEP9_FIELDS_JSON, HashMap::class.java)
    txn = gson.fromJson(TXN_JSON, JdbcSep24Transaction::class.java)
  }

  @Test
  fun `test correct config`() {
    val config =
      gson.fromJson(SIMPLE_CONFIG_JSON, PropertySep24Config.InteractiveUrlConfig::class.java)
    val constructor = SimpleInteractiveUrlConstructor(config, jwtService)
    val url = constructor.construct(txn, "en", sep9Fields as HashMap<String, String>?)
    val params = UriComponentsBuilder.fromUriString(url).build().queryParams
    val cipher = params.get("token")!![0]
    val jwt = Sep24InteractiveUrlJwt(jwtService.decode(cipher))
    val claims = jwt.claims()

    assertEquals("txn_123", jwt.jti as String)
    assertTrue(Instant.ofEpochSecond(jwt.exp).isAfter(Instant.now()))
    assertEquals("deposit", claims.get("kind") as String)
    assertEquals("John Doe", claims.get("name") as String)
    assertEquals(
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
      claims.get("amountInAsset") as String
    )
    assertEquals("en", claims.get("lang") as String)
    assertEquals("john_doe@stellar.org", claims.get("email") as String)
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
  "amount_in": "100",
  "amount_in_asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
}  
"""
