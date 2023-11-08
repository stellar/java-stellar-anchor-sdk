package org.stellar.anchor.platform

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformCustodyEnd2EndTest :
  AbstractIntegrationTest(
    TestConfig(testProfileName = "default-custody").also {
      it.env[RUN_DOCKER] = "true"
      it.env[RUN_ALL_SERVERS] = "false"
      it.env[RUN_SEP_SERVER] = "true"
      it.env[RUN_PLATFORM_SERVER] = "true"
      it.env[RUN_EVENT_PROCESSING_SERVER] = "true"
      it.env[RUN_PAYMENT_OBSERVER] = "true"
      it.env[RUN_CUSTODY_SERVER] = "true"
      it.env[RUN_KOTLIN_REFERENCE_SERVER] = "true"
      it.env[RUN_WALLET_SERVER] = "true"
    }
  ) {

  companion object {
    private val singleton = AnchorPlatformCustodyEnd2EndTest()

    @BeforeAll
    @JvmStatic
    fun construct() {
      println("Running AnchorPlatformCustodyEnd2EndTest")
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
    singleton.sep24CustodyE2eTests.testAll()
  }
}
