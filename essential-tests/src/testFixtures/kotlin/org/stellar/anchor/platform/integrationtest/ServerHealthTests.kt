package org.stellar.anchor.platform.integrationtest

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.platform.TestConfig

class PlatformServerHealthTests {
  val config = TestConfig()
  private val platformApiClient =
    PlatformApiClient(AuthHelper.forNone(), config.env["sep.server.url"]!!)

  @Test
  fun testHealth() {
    val response = platformApiClient.health(listOf("all"))
    Assertions.assertEquals(5, response.size)
    Assertions.assertEquals(0L, response["number_of_checks"])
    Assertions.assertNotNull(response["checks"])
    Assertions.assertNotNull(response["started_at"])
    Assertions.assertNotNull(response["elapsed_time_ms"])
    Assertions.assertNotNull(response["number_of_checks"])
    Assertions.assertNotNull(response["version"])
  }
}
