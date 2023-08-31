package org.stellar.anchor.platform

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformApiRpcEnd2EndTest :
  AbstractIntegrationTest(TestConfig(testProfileName = "default-rpc")) {

  companion object {
    private val singleton = AnchorPlatformApiRpcEnd2EndTest()

    @BeforeAll
    @JvmStatic
    fun construct() {
      println("Running AnchorPlatformApiRpcEnd2EndTest")
      singleton.setUp(mapOf())
    }

    @AfterAll
    @JvmStatic
    fun destroy() {
      singleton.tearDown()
    }
  }

  @Test
  @Order(1)
  fun runSep24Test() {
    singleton.sep24RpcE2eTests.testAll()
  }

  @Test
  @Order(11)
  fun runSep31Test() {
    singleton.sep31RpcE2eTests.testAll()
  }
}
