package org.stellar.anchor.platform

import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformCustodyIntegrationTest :
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
      it.env[RUN_WALLET_SERVER] = "false"
    }
  ) {
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
