package org.stellar.anchor.platform

import org.stellar.anchor.platform.test.*
import org.stellar.anchor.util.Sep1Helper

open class AbstractIntegrationTest(private val config: TestConfig) {
  companion object {
    const val ANCHOR_TO_PLATFORM_SECRET = "myAnchorToPlatformSecret"
    const val PLATFORM_TO_ANCHOR_SECRET = "myPlatformToAnchorSecret"
    const val PLATFORM_TO_CUSTODY_SECRET = "myPlatformToCustodySecret"
    const val PLATFORM_SERVER_PORT = 8085
    const val CUSTODY_SERVER_SERVER_PORT = 8086
    const val REFERENCE_SERVER_PORT = 8081
    const val JWT_EXPIRATION_MILLISECONDS = 10000L
  }

  init {
    System.getProperties()
      .setProperty("REFERENCE_SERVER_CONFIG", "classpath:/anchor-reference-server.yaml")
  }

  val testProfileRunner = TestProfileExecutor(config)
  lateinit var sep6Tests: Sep6Tests
  lateinit var sep10Tests: Sep10Tests
  lateinit var sep12Tests: Sep12Tests
  lateinit var sep24Tests: Sep24Tests
  lateinit var sep31Tests: Sep31Tests
  lateinit var sep38Tests: Sep38Tests
  lateinit var sepHealthTests: SepHealthTests
  lateinit var platformApiTests: PlatformApiTests
  lateinit var platformApiCustodyTests: PlatformApiCustodyTests
  lateinit var callbackApiTests: CallbackApiTests
  lateinit var stellarObserverTests: StellarObserverTests
  lateinit var custodyApiTests: CustodyApiTests
  lateinit var eventProcessingServerTests: EventProcessingServerTests
  lateinit var sep24E2eTests: Sep24End2EndTest
  lateinit var sep24RpcE2eTests: Sep24RpcEnd2EndTests
  lateinit var sep24CustodyE2eTests: Sep24CustodyEnd2EndTests
  lateinit var sep24CustodyRpcE2eTests: Sep24CustodyRpcEnd2EndTests

  fun setUp(envMap: Map<String, String>) {
    envMap.forEach { (key, value) -> config.env[key] = value }
    testProfileRunner.start()
    setupTests()
  }

  fun tearDown() {
    testProfileRunner.shutdown()
  }

  private fun setupTests() {
    // Query SEP-1
    val toml =
      Sep1Helper.parse(resourceAsString("${config.env["anchor.domain"]}/.well-known/stellar.toml"))

    // Create Sep10Tests
    sep10Tests = Sep10Tests(toml)

    // Get JWT
    val jwt = sep10Tests.sep10Client.auth()

    sep6Tests = Sep6Tests(toml)
    sep12Tests = Sep12Tests(config, toml, jwt)
    sep24Tests = Sep24Tests(config, toml, jwt)
    sep31Tests = Sep31Tests(config, toml, jwt)
    sep38Tests = Sep38Tests(config, toml, jwt)
    sepHealthTests = SepHealthTests(config, toml, jwt)
    platformApiTests = PlatformApiTests(config, toml, jwt)
    platformApiCustodyTests = PlatformApiCustodyTests(config, toml, jwt)
    callbackApiTests = CallbackApiTests(config, toml, jwt)
    stellarObserverTests = StellarObserverTests()
    custodyApiTests = CustodyApiTests(config, toml, jwt)
    sep24E2eTests = Sep24End2EndTest(config, jwt)
    sep24CustodyE2eTests = Sep24CustodyEnd2EndTests(config, jwt)
    sep24RpcE2eTests = Sep24RpcEnd2EndTests(config, jwt)
    sep24CustodyRpcE2eTests = Sep24CustodyRpcEnd2EndTests(config, jwt)
    eventProcessingServerTests = EventProcessingServerTests(config, toml, jwt)
  }
}
