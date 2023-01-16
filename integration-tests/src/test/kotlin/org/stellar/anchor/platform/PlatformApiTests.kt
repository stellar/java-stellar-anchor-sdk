package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService

lateinit var platformApiClient: PlatformApiClient

class PlatformApiTests {
  companion object {
    fun setup() {
      if (!::platformApiClient.isInitialized) {
        val platformToAnchorJwtService = JwtService("myAnchorToPlatformSecret")
        val authHelper =
          AuthHelper.forJwtToken(platformToAnchorJwtService, 900000, "http://localhost:8081")
        platformApiClient = PlatformApiClient(authHelper, "http://localhost:8080")
      }
    }

    fun testHealth() {
      val response = platformApiClient.health(listOf("all"))
      assertEquals(5, response.size)
      assertEquals(1.0, response["number_of_checks"])
      assertNotNull(response["checks"])
      assertNotNull(response["started_at"])
      assertNotNull(response["elapsed_time_ms"])
      assertNotNull(response["number_of_checks"])
      assertNotNull(response["version"])
    }
  }
}

fun platformTestAll() {
  PlatformApiTests.setup()
  println("Performing Platform API tests...")

  PlatformApiTests.testHealth()
}
