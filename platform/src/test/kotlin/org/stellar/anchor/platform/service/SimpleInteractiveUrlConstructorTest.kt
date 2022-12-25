package org.stellar.anchor.platform.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.JwtToken
import org.stellar.anchor.platform.config.PropertySep24Config
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.util.GsonUtils

@Suppress("UNCHECKED_CAST")
class SimpleInteractiveUrlConstructorTest {
  companion object {
    private val gson = GsonUtils.getInstance()
  }

  @MockK(relaxed = true) private lateinit var jwtService: JwtService
  lateinit var jwtToken: JwtToken
  lateinit var sep9Fields: HashMap<*, *>
  lateinit var txn: JdbcSep24Transaction

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    every { jwtService.encode(any()) } returns "mock_token"

    jwtToken = JwtToken()
    sep9Fields = gson.fromJson(sep9FieldsJson, HashMap::class.java)
    txn = gson.fromJson(txnJson, JdbcSep24Transaction::class.java)
  }

  @Test
  fun `test correct config`() {
    val config =
      gson.fromJson(simpleConfig, PropertySep24Config.SimpleInteractiveUrlConfig::class.java)
    val constructor = SimpleInteractiveUrlConstructor(config, jwtService)
    val url = constructor.construct(jwtToken, txn, "en", sep9Fields as HashMap<String, String>?)
    assertEquals(wantedTestUrl, url)
  }
}

private const val simpleConfig =
  """
{
  "baseUrl": "http://localhost:8080/sep24/interactive",
  "txnFields": [
    "kind",
    "amount_in",
    "amount_in_asset",
    "asset_code"
  ]
}
"""

private const val sep9FieldsJson =
  """
{
  "name": "John Doe",
  "email": "john_doe@stellar.org"
}
"""

private const val txnJson =
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

private const val wantedTestUrl =
  """http://localhost:8080/sep24/interactive?transaction_id=txn_123&token=mock_token&lang=en&name=John+Doe&email=john_doe%40stellar.org&kind=deposit&amountIn=100&amountInAsset=stellar%3AUSDC%3AGDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"""
