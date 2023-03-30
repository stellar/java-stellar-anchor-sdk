package org.stellar.anchor.platform.test

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.platform.CLIENT_WALLET_SECRET
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.Sep1Helper
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.anchor.TransactionStatus
import org.stellar.walletsdk.asset.IssuedAssetId
import org.stellar.walletsdk.auth.AuthToken
import org.stellar.walletsdk.horizon.SigningKeyPair

class Sep24End2EndTest(
  private val config: TestConfig,
  private val toml: Sep1Helper.TomlContent,
  private val jwt: String
) {
  private val walletSecretKey = System.getenv("WALLET_SECRET_KEY") ?: CLIENT_WALLET_SECRET
  private val keypair = SigningKeyPair.fromSecret(walletSecretKey)
  private val wallet =
    Wallet(StellarConfiguration.Testnet, ApplicationConfiguration(useHttp = true))
  private val USDC =
    IssuedAssetId("USDC", "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP")
  private val asset = USDC
  private val client =
    HttpClient() {
      install(HttpTimeout) {
        requestTimeoutMillis = 300000
        connectTimeoutMillis = 300000
        socketTimeoutMillis = 300000
      }
    }
  private val anchor = wallet.anchor(config.env["anchor.domain"]!!.substring("http://".length))

  private val maxTries = 40

  private fun `typical deposit end-to-end flow`() = runBlocking {
    val token = anchor.auth().authenticate(keypair)
    // Start interactive deposit
    val deposit =
      anchor.interactive().deposit(keypair.address, asset, token, mapOf("amount" to "10"))
    val transaction = anchor.getTransaction(deposit.id, token)
    assertEquals(TransactionStatus.INCOMPLETE, transaction.status)

    val resp = client.get(deposit.url)
    print("accessing ${deposit.url}...")
    assertEquals(200, resp.status.value)
    waitStatus(deposit.id, TransactionStatus.COMPLETED, token)
  }

  private suspend fun waitStatus(id: String, expectedStatus: TransactionStatus, token: AuthToken) {
    var status: TransactionStatus? = null

    for (i in 0..maxTries) {
      // Get transaction info
      val transaction = anchor.getTransaction(id, token)

      if (status != transaction.status) {
        status = transaction.status
        println("Deposit transaction status changed to $status. Message: ${transaction.message}")
      }

      delay(1.seconds)

      if (transaction.status == expectedStatus) {
        return
      }
    }

    fail("Transaction wasn't $expectedStatus in $maxTries tries, last status: $status")
  }

  fun testAll() {
    println("Running SEP-24 end-to-end tests...")
    `typical deposit end-to-end flow`()
  }
}
