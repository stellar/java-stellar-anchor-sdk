package org.stellar.anchor.platform

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.stellar.anchor.reference.AnchorReferenceServer
import org.stellar.anchor.util.Sep1Helper

class AnchorPlatformIntegrationTest {
  companion object {
    const val SEP_SERVER_PORT = 8080
    const val REFERENCE_SERVER_PORT = 8081
    lateinit var toml: Sep1Helper.TomlContent
    lateinit var jwt: String

    @BeforeAll
    @JvmStatic
    fun setup() {
      AnchorPlatformServer.start(
        SEP_SERVER_PORT,
        "/",
        mapOf("stellar.anchor.config" to "classpath:test-anchor-config.yaml")
      )

      AnchorReferenceServer.start(REFERENCE_SERVER_PORT, "/")
    }

    @AfterAll fun tearDown() {}
  }

  private fun readSep1Toml(): Sep1Helper.TomlContent {
    val tomlString = resourceAsString("http://localhost:$SEP_SERVER_PORT/.well-known/stellar.toml")
    return Sep1Helper.parse(tomlString)
  }

  @Test
  @Order(1)
  fun runSep1Test() {
    toml = readSep1Toml()
  }

  @Test
  @Order(2)
  fun runSep10Test() {
    jwt = sep10TestAll(toml)
  }

  @Test
  @Order(3)
  fun runSep12Test() {
    sep12TestAll(toml, jwt)
  }

  @Test
  @Order(4)
  fun runSep38Test() {
    sep38TestAll(toml, jwt)
  }
}
