package org.stellar.anchor.platform

val PostgresConfig =
  TestConfig().also {
    it.env[RUN_ALL_SERVERS] = "false"
    it.env[RUN_SEP_SERVER] = "true"
    it.env["data.flyway_enabled"] = "true"
  }

// class PostgresMigrationTest : AbstractIntegrationTest(PostgresConfig) {
//  companion object {
//    private val singleton = PostgresMigrationTest()
//
//    @BeforeAll
//    @JvmStatic
//    fun construct() {
//      println("Running PostgresMigrationTest")
//      singleton.testProfileRunner.start()
//    }
//
//    @AfterAll
//    @JvmStatic
//    fun destroy() {
//      singleton.testProfileRunner.shutdown()
//    }
//  }
//
//  @Test
//  fun test() {
//    // Nothing to do
//  }
// }
