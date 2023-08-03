package org.stellar.anchor.platform.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.time.Instant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.util.UriComponentsBuilder
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.Sep10Jwt
import org.stellar.anchor.auth.Sep24MoreInfoUrlJwt
import org.stellar.anchor.config.SecretConfig
import org.stellar.anchor.platform.config.PropertySep24Config
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.util.GsonUtils

class SimpleMoreInfoUrlConstructorTest {
  companion object {
    private val gson = GsonUtils.getInstance()
  }

  @MockK(relaxed = true) private lateinit var secretConfig: SecretConfig
  private lateinit var jwtService: JwtService
  private lateinit var sep10Jwt: Sep10Jwt
  private lateinit var txn: JdbcSep24Transaction

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { secretConfig.sep24MoreInfoUrlJwtSecret } returns "sep24_jwt_secret"

    jwtService = JwtService(secretConfig)
    txn = gson.fromJson(TXN_JSON, JdbcSep24Transaction::class.java)
  }

  @Test
  fun `test correct config`() {
    val config =
      gson.fromJson(SIMPLE_CONFIG_JSON, PropertySep24Config.MoreInfoUrlConfig::class.java)
    val constructor = SimpleMoreInfoUrlConstructor(config, jwtService)
    val url = constructor.construct(txn)

    val params = UriComponentsBuilder.fromUriString(url).build().queryParams
    val cipher = params["token"]!![0]
    val jwt = jwtService.decode(cipher, Sep24MoreInfoUrlJwt::class.java)

    assertEquals("txn_123", jwt.jti as String)
    Assertions.assertTrue(Instant.ofEpochSecond(jwt.exp).isAfter(Instant.now()))
    assertEquals("GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO:1234", jwt.sub)
    assertEquals("lobstr.co", jwt.claims["client_domain"])
  }
}

private const val SIMPLE_CONFIG_JSON =
  """
{
  "baseUrl": "http://localhost:8080/sep24/more_info_url",
  "jwtExpiration": 600,
  "txnFields": [
    "kind",
    "status"
  ]
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
  "amount_in_asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
  "sep10_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
  "sep10_account_memo": "1234",
  "client_domain": "lobstr.co"
}  
"""
