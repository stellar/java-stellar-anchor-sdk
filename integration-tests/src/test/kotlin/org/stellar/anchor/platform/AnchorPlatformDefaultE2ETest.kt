package org.stellar.anchor.platform

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformDefaultE2ETest :
  AbstractIntegrationTest(TestConfig(testProfileName = "default")) {

  companion object {
    private val singleton = AnchorPlatformDefaultE2ETest()

    @BeforeAll
    @JvmStatic
    fun construct() {
      singleton.setUp()
    }

    @AfterAll
    @JvmStatic
    fun destroy() {
      singleton.tearDown()
    }
  }

  @Test
  @Order(1)
  fun runSep6Test() {
    singleton.sep6E2eTests.testAll()
  }
}
