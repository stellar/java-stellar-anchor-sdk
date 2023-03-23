package org.stellar.anchor.platform

import java.io.File
import org.stellar.anchor.platform.test.*
import org.stellar.anchor.util.Sep1Helper

data class TestConfig(val profileName: String) {
  val env = mutableMapOf<String, String>()
  val testEnvFile = getResourceFilePath("profiles/${profileName}/test.env")
  val serversEnvFile = getResourceFilePath("profiles/${profileName}/servers.env")
  init {
    // override test profile name with TEST_PROFILE_NAME env variable
    val testProfileName = System.getenv("TEST_PROFILE_NAME") ?: profileName
    // read test.env file
    val testEnv = readResourceAsMap(File(testEnvFile))
    // read servers.env file
    val serversEnv = readResourceAsMap(File(serversEnvFile))
    // merge test.env and servers.env
    env.putAll(testEnv)
    env.putAll(serversEnv)
    env.putAll(System.getenv())
  }
}

open class AbstractIntegrationTest(private val config: TestConfig) {
  init {
    System.getProperties()
      .setProperty("REFERENCE_SERVER_CONFIG", "classpath:/anchor-reference-server.yaml")
  }

  private val testEnvRunner = TestEnvRunner(config)
  lateinit var sep10Tests: Sep10Tests
  lateinit var sep12Tests: Sep12Tests
  lateinit var sep24Tests: Sep24Tests
  lateinit var sep31Tests: Sep31Tests
  lateinit var sep38Tests: Sep38Tests
  lateinit var platformApiTests: PlatformApiTests
  lateinit var callbackApiTests: CallbackApiTests
  lateinit var stellarObserverTests: StellarObserverTests
  lateinit var sep24E2eTests: Sep24End2EndTest

  fun setUp() {
    testEnvRunner.start()
    setupTests()
  }

  fun tearDown() {
    testEnvRunner.shutdown()
  }

  private fun setupTests() {
    // Query SEP-1
    val toml =
      Sep1Helper.parse(resourceAsString("${config.env["anchor.domain"]}/.well-known/stellar.toml"))

    // Create Sep10Tests
    sep10Tests = Sep10Tests(toml)

    // Get JWT
    val jwt = sep10Tests.sep10Client.auth()

    sep12Tests = Sep12Tests(config, toml, jwt)
    sep24Tests = Sep24Tests(config, toml, jwt)
    sep31Tests = Sep31Tests(config, toml, jwt)
    sep38Tests = Sep38Tests(config, toml, jwt)
    platformApiTests = PlatformApiTests(config, toml, jwt)
    callbackApiTests = CallbackApiTests(config, toml, jwt)
    stellarObserverTests = StellarObserverTests()
    sep24E2eTests = Sep24End2EndTest(config, toml, jwt)
  }
}
