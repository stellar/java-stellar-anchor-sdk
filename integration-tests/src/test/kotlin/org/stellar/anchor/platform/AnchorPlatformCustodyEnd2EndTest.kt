package org.stellar.anchor.platform

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformCustodyEnd2EndTest :
  AbstractIntegrationTest(TestConfig(testProfileName = "default-custody")) {

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

  @Test
  @Order(11)
  fun runSep6Test() {
    // The SEP-6 reference server implementation only implements RPC, so technically this test
    // should be in the RPC test suite.
    singleton.sep6E2eTests.testAll()
  }
}
