package org.stellar.anchor.platform

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformActionsEnd2EndTest :
  AbstractIntegrationTest(TestConfig(testProfileName = "sep24-actions")) {

  companion object {
    private val singleton = AnchorPlatformActionsEnd2EndTest()

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
    singleton.sep24ActionsE2eTests.testAll()
  }
}
