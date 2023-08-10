package org.stellar.anchor.util

import java.io.IOException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.exception.InvalidConfigException

class Sep1HelperTest {
  val stellarToml =
    """
          ACCOUNTS = [ "GCSGSR6KQQ5BP2FXVPWRL6SWPUSFWLVONLIBJZUKTVQB5FYJFVL6XOXE" ]
          VERSION = "0.1.0"
          SIGNING_KEY = "GBDYDBJKQBJK4GY4V7FAONSFF2IBJSKNTBYJ65F5KCGBY2BIGPGGLJOH"
          NETWORK_PASSPHRASE = "Test SDF Network ; September 2015"
          
          WEB_AUTH_ENDPOINT = "http://localhost:8080/auth"
          KYC_SERVER = "http://localhost:8080/sep12"
          TRANSFER_SERVER_SEP0024 = "http://localhost:8080/sep24"
          DIRECT_PAYMENT_SERVER = "http://localhost:8080/sep31"
          ANCHOR_QUOTE_SERVER = "http://localhost:8080/sep38"
          
          [[CURRENCIES]]
          code = "SRT"
          issuer = "GCDNJUBQSX7AJWLJACMJ7I4BC3Z47BQUTMHEICZLE6MU4KQBRYG5JY6B"
          status = "test"
          is_asset_anchored = false
          anchor_asset_type = "other"
          desc = "A fake anchored asset to use with this example anchor server."
          
          [DOCUMENTATION]
          ORG_NAME = "Stellar Development Foundation"
          ORG_URL = "https://stellar.org"
          ORG_DESCRIPTION = "SEP 24 reference server."
          ORG_KEYBASE = "stellar.public"
          ORG_TWITTER = "StellarOrg"
          ORG_GITHUB = "stellar"
        """
      .trimIndent()

  private val mockWebServer = MockWebServer()

  @BeforeEach
  fun setup() {
    mockWebServer.start()
  }

  @AfterEach
  fun teardown() {
    mockWebServer.shutdown()
  }

  @Test
  fun `test Read Toml with IOException`() {
    // Enqueue a response with an HTTP error status code
    mockWebServer.enqueue(MockResponse().setResponseCode(500))
    val exception =
      assertThrows(IOException::class.java) {
        Sep1Helper.readToml(mockWebServer.url("/").toString())
      }
    // You may need to adjust the assertion based on the specific behavior of Sep1Helper
    assertTrue(exception.message!!.contains("An error occurred while fetching the TOML from"))
  }

  @Test
  fun `test Parse Invalid Toml String`() {
    val invalidTomlString = "key = value" // An invalid TOML string without quotes
    val exception =
      assertThrows(InvalidConfigException::class.java) { Sep1Helper.parse(invalidTomlString) }
    assertEquals("Failed to parse TOML content. Invalid Config.", exception.message)
  }
}
