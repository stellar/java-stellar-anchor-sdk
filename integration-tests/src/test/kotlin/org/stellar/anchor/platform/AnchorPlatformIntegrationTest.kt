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
  @Order(11)
  fun runSep10Test() {
    singleton.sep10Tests.testAll()
  }

  @Test
  @Order(12)
  fun runSep12Test() {
    singleton.sep12Tests.testAll()
  }

  @Test
  @Order(13)
  fun runSep24Test() {
    singleton.sep24Tests.testAll()
  }

  @Test
  @Order(14)
  fun runSep31Test() {
    singleton.sep31Tests.testAll()
  }

  @Test
  @Order(15)
  fun runSep38Test() {
    singleton.sep38Tests.testAll()
  }

  @Test
  @Order(16)
  fun runSep6Test() {
    singleton.sep6Tests.testAll()
  }

  @Test
  @Order(21)
  fun runSepHealthTest() {
    singleton.sepHealthTests.testAll()
  }

  @Test
  @Order(31)
  fun runPlatformApiTest() {
    singleton.platformApiTests.testAll()
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
