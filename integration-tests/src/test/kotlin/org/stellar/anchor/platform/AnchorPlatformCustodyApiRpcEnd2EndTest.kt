package org.stellar.anchor.platform

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformCustodyApiRpcEnd2EndTest :
  AbstractIntegrationTest(TestConfig(testProfileName = "sep24-custody-rpc")) {

  companion object {
    private val singleton = AnchorPlatformCustodyApiRpcEnd2EndTest()

    @BeforeAll
    @JvmStatic
    fun construct() {
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
}
