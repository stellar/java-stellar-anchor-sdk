package org.stellar.anchor.platform.test

import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlin.test.DefaultAsserter.fail
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.api.sep.sep6.GetTransactionResponse
import org.stellar.anchor.platform.CLIENT_WALLET_SECRET
import org.stellar.anchor.platform.Sep6Client
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.Log
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.horizon.SigningKeyPair

class Sep6End2EndTest(config: TestConfig, val jwt: String) {
  private val walletSecretKey = System.getenv("WALLET_SECRET_KEY") ?: CLIENT_WALLET_SECRET
  private val keypair = SigningKeyPair.fromSecret(walletSecretKey)
  private val wallet =
    Wallet(
      StellarConfiguration.Testnet,
      ApplicationConfiguration { defaultRequest { url { protocol = URLProtocol.HTTP } } }
    )
  private val anchor =
    wallet.anchor(config.env["anchor.domain"]!!) {
      install(HttpTimeout) {
        requestTimeoutMillis = 10000
        connectTimeoutMillis = 10000
        socketTimeoutMillis = 10000
      }
    }

  private fun `test typical deposit end-to-end flow`() = runBlocking {
    val token = anchor.auth().authenticate(keypair).token
    val sep6Client = Sep6Client("http://localhost:8080/sep6", token)

    val deposit =
      sep6Client.deposit(
        mapOf(
          "asset_code" to "USDC",
          "account" to keypair.address,
          "amount" to "0.01",
          "type" to "bank_account"
        )
      )
    waitStatus(deposit.id, "completed", sep6Client)

    val fetchedTxn = sep6Client.getTransaction(mapOf("id" to deposit.id))
    Log.info("Fetched transaction: $fetchedTxn")
    val transactionByStellarId: GetTransactionResponse =
      sep6Client.getTransaction(
        mapOf("stellar_transaction_id" to fetchedTxn.transaction.stellarTransactionId)
      )
    assertEquals(fetchedTxn.transaction.id, transactionByStellarId.transaction.id)
  }

  private suspend fun waitStatus(id: String, expectedStatus: String, sep6Client: Sep6Client) {
    for (i in 0..40) {
      val transaction = sep6Client.getTransaction(mapOf("id" to id))
      if (expectedStatus != transaction.transaction.status) {
        Log.info("Transaction status: ${transaction.transaction.status}")
      } else {
        Log.info(
          "Transaction status ${transaction.transaction.status} matched expected status $expectedStatus"
        )
        return
      }
      delay(1.seconds)
    }
    fail("Transaction status did not match expected status $expectedStatus")
  }

  private fun `test asynchronous deposit end-to-end flow`() = runBlocking {}

  fun testAll() {
    `test typical deposit end-to-end flow`()
    `test asynchronous deposit end-to-end flow`()
  }
}
