package org.stellar.anchor.client

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

val PostgresConfig =
  TestConfig("default").also {
    it.env[RUN_DOCKER] = "false"
    it.env[RUN_ALL_SERVERS] = "false"
    it.env[RUN_SEP_SERVER] = "true"
    it.env["data.flyway_enabled"] = "true"
  }

// Temporarily disable this test because we can only run test server in the default profile at this
// moment. This will be moved to extended tests.
@Disabled
class PostgresMigrationTest : AbstractIntegrationTest(PostgresConfig) {
  companion object {
    private val singleton = PostgresMigrationTest()

    @BeforeAll
    @JvmStatic
    fun construct() {
      println("Running PostgresMigrationTest")
      singleton.testProfileRunner.start()
    }

    @AfterAll
    @JvmStatic
    fun destroy() {
      singleton.testProfileRunner.shutdown()
    }
  }

  @Test
  fun test() {
    // Nothing to do
  }
}
