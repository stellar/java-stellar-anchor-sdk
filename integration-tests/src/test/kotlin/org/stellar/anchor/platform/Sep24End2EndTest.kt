package org.stellar.anchor.platform

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.stellar.anchor.util.Sep1Helper.TomlContent
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.asset.IssuedAssetId

class Sep24End2EndTest(
  private val config: TestConfig,
  private val toml: TomlContent,
  private val jwt: String
) {
  private val walletSecretKey = System.getenv("WALLET_SECRET_KEY") ?: CLIENT_WALLET_SECRET
  private val wallet = Wallet(StellarConfiguration.Testnet)
  private val stellar = wallet.stellar()
  private val account = wallet.stellar().account()
  private val keypair = account.createKeyPair()
  private val anchor = wallet.anchor(config.env["anchor.domain"]!!)
  private val USDC =
    IssuedAssetId("USDC", "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
  private val asset = USDC
  private fun `typical deposit end-to-end flow`() = runBlocking {
    val token = anchor.auth().authenticate(keypair)
    val info = anchor.getInfo()
    val deposit = anchor.interactive().deposit(keypair.address, asset, authToken = token)
  }
}

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Sep24End2EndTestJunit :
  AbstractIntegrationTest(
    TestConfig(
      profileName = "sep24"
    )
  ) {

  companion object {
    private val singleton = Sep24End2EndTestJunit()

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
  fun runSep24Test() {
//    instance.sep24Ee2eTests.testAll()
  }
}
