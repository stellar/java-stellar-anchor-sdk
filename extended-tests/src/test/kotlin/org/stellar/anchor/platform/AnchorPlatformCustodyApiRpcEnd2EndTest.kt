package org.stellar.anchor.platform

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformCustodyApiRpcEnd2EndTest :
  AbstractIntegrationTest(
    TestConfig(testProfileName = "default-custody-rpc").also {
      it.env[RUN_DOCKER] = "true"
      it.env[RUN_ALL_SERVERS] = "false"
      it.env[RUN_SEP_SERVER] = "true"
      it.env[RUN_PLATFORM_SERVER] = "true"
      it.env[RUN_EVENT_PROCESSING_SERVER] = "true"
      it.env[RUN_PAYMENT_OBSERVER] = "true"
      it.env[RUN_CUSTODY_SERVER] = "true"
      it.env[RUN_KOTLIN_REFERENCE_SERVER] = "true"
    }
  ) {

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
