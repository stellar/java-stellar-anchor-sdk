package org.stellar.anchor.platform

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformCustodyActionsEnd2EndTest :
  AbstractIntegrationTest(TestConfig(testProfileName = "sep24-custody-actions")) {

  companion object {
    private val singleton = AnchorPlatformCustodyActionsEnd2EndTest()

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
    singleton.sep24CustodyActionsE2eTests.testAll()
  }
}
