package org.stellar.anchor.platform.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.Sep1Helper.TomlContent

class SepHealthTests(config: TestConfig, toml: TomlContent, jwt: String) {
  private val platformApiClient =
    PlatformApiClient(AuthHelper.forNone(), config.env["sep.server.url"]!!)

  private fun testHealth() {
    val response = platformApiClient.health(listOf("all"))
    assertEquals(5, response.size)
    assertEquals(1.0, response["number_of_checks"])
    assertNotNull(response["checks"])
    assertNotNull(response["started_at"])
    assertNotNull(response["elapsed_time_ms"])
    assertNotNull(response["number_of_checks"])
    assertNotNull(response["version"])
  }
  fun testAll() {
    println("Performing Sep Sever Health tests...")
    testHealth()
  }
}
