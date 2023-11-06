package org.stellar.anchor.client

import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
// Temporarily disable this test because we can only run test server in the default profile at this
// moment. This will be moved to extended tests.
@Disabled
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
}
