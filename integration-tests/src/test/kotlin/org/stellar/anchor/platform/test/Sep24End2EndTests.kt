package org.stellar.anchor.platform.test

import kotlinx.coroutines.runBlocking
import org.stellar.anchor.platform.CLIENT_WALLET_SECRET
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.Sep1Helper
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.asset.IssuedAssetId

class Sep24End2EndTest(
  private val config: TestConfig,
  private val toml: Sep1Helper.TomlContent,
  private val jwt: String
) {
  private val walletSecretKey = System.getenv("WALLET_SECRET_KEY") ?: CLIENT_WALLET_SECRET
  private val wallet =
    Wallet(StellarConfiguration.Testnet, ApplicationConfiguration(useHttp = true))
  private val stellar = wallet.stellar()
  private val account = wallet.stellar().account()
  private val keypair = account.createKeyPair()
  private val anchor = wallet.anchor(config.env["anchor.domain"]!!.substring("http://".length))
  private val USDC =
    IssuedAssetId("USDC", "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP")
  private val asset = USDC
  private fun `typical deposit end-to-end flow`() = runBlocking {
    val token = anchor.auth().authenticate(keypair)
    val info = anchor.getInfo()
    val deposit = anchor.interactive().deposit(keypair.address, asset, authToken = token)
  }

  fun testAll() {
    println("Running SEP-24 end-to-end tests...")
    `typical deposit end-to-end flow`()
  }
}
