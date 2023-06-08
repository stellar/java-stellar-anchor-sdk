package org.stellar.anchor.platform

import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.stellar.anchor.platform.test.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformCustodyIntegrationTest :
  AbstractIntegrationTest(TestConfig(profileName = "default-custody")) {
  companion object {
    private val singleton = AnchorPlatformCustodyIntegrationTest()
    private val custodyMockServer = MockWebServer()

    @BeforeAll
    @JvmStatic
    fun construct() {
      custodyMockServer.start()
      singleton.setUp(mapOf("custody.fireblocks.base_url" to custodyMockServer.url("").toString()))
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
}
