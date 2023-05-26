package org.stellar.anchor.platform

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class JwtAuthIntegrationTest {
  companion object {
    private val testProfileRunner =
      TestProfileExecutor(
        TestConfig(profileName = "default").also {
          // enable platform server jwt auth
          it.env["platform_server.auth.type"] = "jwt"
        }
      )

    @BeforeAll
    fun setup() {
      testProfileRunner.start()
    }

    @AfterAll
    fun breakdown() {
      testProfileRunner.shutdown()
    }

    @Test fun `test JWT auth to platform server`() {}
  }
}
