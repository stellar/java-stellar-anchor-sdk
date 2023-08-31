package org.stellar.anchor.platform

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformCustodyApiRpcEnd2EndTest :
  AbstractIntegrationTest(TestConfig(testProfileName = "default-custody-rpc")) {

  companion object {
    private val singleton = AnchorPlatformCustodyApiRpcEnd2EndTest()

    @BeforeAll
    @JvmStatic
    fun construct() {
      println("Running AnchorPlatformCustodyApiRpcEnd2EndTest")
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
    singleton.sep24CustodyRpcE2eTests.testAll()
  }

  @Test
  @Order(11)
  fun runSep31Test() {
    singleton.sep31CustodyRpcE2eTests.testAll()
  }
}
