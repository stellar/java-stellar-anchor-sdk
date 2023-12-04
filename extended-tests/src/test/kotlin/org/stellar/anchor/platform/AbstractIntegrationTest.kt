package org.stellar.anchor.platform

import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.platform.test.CustodyApiTests
import org.stellar.anchor.platform.test.PlatformApiCustodyTests
import org.stellar.anchor.platform.test.Sep24BaseEnd2EndTest
import org.stellar.anchor.platform.test.Sep31End2EndTests
import org.stellar.anchor.util.Sep1Helper
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.anchor.auth
import org.stellar.walletsdk.horizon.SigningKeyPair

open class AbstractIntegrationTest(private val config: TestConfig) {
  companion object {
    const val ANCHOR_TO_PLATFORM_SECRET = "myAnchorToPlatformSecret"
    const val PLATFORM_TO_ANCHOR_SECRET = "myPlatformToAnchorSecret"
    const val PLATFORM_TO_CUSTODY_SECRET = "myPlatformToCustodySecret"
    const val PLATFORM_SERVER_PORT = 8085
    const val CUSTODY_SERVER_SERVER_PORT = 8086
    const val REFERENCE_SERVER_PORT = 8091
    const val JWT_EXPIRATION_MILLISECONDS = 10000L
  }

  init {
    System.getProperties()
      .setProperty("REFERENCE_SERVER_CONFIG", "classpath:/anchor-reference-server.yaml")
  }

  val testProfileRunner = TestProfileExecutor(config)
  lateinit var platformApiCustodyTests: PlatformApiCustodyTests
  lateinit var custodyApiTests: CustodyApiTests
  lateinit var sep24RpcE2eTests: Sep24BaseEnd2EndTest
  lateinit var sep31RpcE2eTests: Sep31End2EndTests

  fun setUp(envMap: Map<String, String>) {
    envMap.forEach { (key, value) -> config.env[key] = value }
    testProfileRunner.start()
    setupTests()
  }

  fun tearDown() {
    testProfileRunner.shutdown()
  }

  private fun setupTests() = runBlocking {
    // Query SEP-1
    val toml =
      Sep1Helper.parse(resourceAsString("${config.env["anchor.domain"]}/.well-known/stellar.toml"))

    // Get JWT
    val jwt = auth()

    platformApiCustodyTests = PlatformApiCustodyTests(config, toml, jwt)
    custodyApiTests = CustodyApiTests(config, toml, jwt)
    sep24RpcE2eTests = Sep24BaseEnd2EndTest(config, jwt)
    sep31RpcE2eTests = Sep31End2EndTests(config, toml, jwt)
  }

  private suspend fun auth(): String {
    val wallet =
      Wallet(
        StellarConfiguration.Testnet,
        ApplicationConfiguration { defaultRequest { url { protocol = URLProtocol.HTTP } } }
      )
    val walletKeyPair = SigningKeyPair.fromSecret(CLIENT_WALLET_SECRET)
    val anchor = wallet.anchor(config.env["anchor.domain"]!!)
    return anchor.auth().authenticate(walletKeyPair).token
  }
}
