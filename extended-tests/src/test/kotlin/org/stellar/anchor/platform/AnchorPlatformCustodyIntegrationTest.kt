package org.stellar.anchor.platform

import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
// Temporarily disable this test because we can only run test server in the default profile at this
// moment. This will be moved to extended tests.
@Disabled
class AnchorPlatformCustodyIntegrationTest :
  AbstractIntegrationTest(TestConfig(testProfileName = "default-custody")) {
  companion object {
    private val singleton = AnchorPlatformCustodyIntegrationTest()
    private val custodyMockServer = MockWebServer()

    @BeforeAll
    @JvmStatic
    fun construct() {
      println("Running AnchorPlatformCustodyIntegrationTest")
      custodyMockServer.start()
      val mockServerUrl = custodyMockServer.url("").toString()
      singleton.setUp(mapOf("custody.fireblocks.base_url" to mockServerUrl))
    }

    @AfterAll
    @JvmStatic
    fun destroy() {
      custodyMockServer.shutdown()
      singleton.tearDown()
    }
  }

  @Test
  @Order(1)
  fun runCustodyApiTest() {
    singleton.custodyApiTests.testAll(custodyMockServer)
  }

  @Test
  @Order(11)
  fun runPlatformApiCustodyTest() {
    singleton.platformApiCustodyTests.testAll(custodyMockServer)
  }
}
