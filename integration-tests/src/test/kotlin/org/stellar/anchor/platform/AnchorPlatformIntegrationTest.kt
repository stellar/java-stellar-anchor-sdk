package org.stellar.anchor.platform

import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.stellar.anchor.platform.test.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformIntegrationTest :
  AbstractIntegrationTest(TestConfig(testProfileName = "default")) {
  companion object {
    private val singleton = AnchorPlatformIntegrationTest()
    private val custodyMockServer = MockWebServer()

    @BeforeAll
    @JvmStatic
    fun construct() {
      println("Running AnchorPlatformIntegrationTest")
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
  @Order(16)
  fun runSep6Test() {
    singleton.sep6Tests.testAll()
  }

  @Test
  @Order(41)
  fun runCallbackApiTest() {
    singleton.callbackApiTests.testAll()
  }

  @Test
  @Order(51)
  fun runStellarObserverTest() {
    singleton.stellarObserverTests.testAll()
  }

  @Test
  @Order(61)
  fun runEventProcessingServerTest() {
    singleton.eventProcessingServerTests.testAll()
  }
}
