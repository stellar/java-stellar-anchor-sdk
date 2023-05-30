package org.stellar.anchor.platform

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.stellar.anchor.platform.test.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformIntegrationTest : AbstractIntegrationTest(TestConfig(profileName = "default")) {
  companion object {
    private val singleton = AnchorPlatformIntegrationTest()

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
  fun runSep10Test() {
    singleton.sep10Tests.testAll()
  }

  @Test
  @Order(2)
  fun runSep12Test() {
    singleton.sep12Tests.testAll()
  }

  @Test
  @Order(3)
  fun runSep24Test() {
    singleton.sep24Tests.testAll()
  }

  @Test
  @Order(4)
  fun runSep31Test() {
    singleton.sep31Tests.testAll()
  }

  @Test
  @Order(5)
  fun runSep38Test() {
    singleton.sep38Tests.testAll()
  }

  @Test
  @Order(6)
  fun runSepHealthTest() {
    singleton.sepHealthTests.testAll()
  }

  @Test
  @Order(7)
  fun runPlatformApiTest() {
    singleton.platformApiTests.testAll()
  }

  @Test
  @Order(8)
  fun runCallbackApiTest() {
    singleton.callbackApiTests.testAll()
  }

  @Test
  @Order(9)
  fun runStellarObserverTest() {
    singleton.stellarObserverTests.testAll()
  }
}
