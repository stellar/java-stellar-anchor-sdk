package org.stellar.anchor.platform.test

import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlin.test.DefaultAsserter.fail
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.api.sep.sep6.GetTransactionResponse
import org.stellar.anchor.api.shared.InstructionField
import org.stellar.anchor.platform.CLIENT_WALLET_SECRET
import org.stellar.anchor.platform.Sep6Client
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.Log
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.anchor.MemoType
import org.stellar.walletsdk.anchor.auth
import org.stellar.walletsdk.anchor.customer
import org.stellar.walletsdk.asset.IssuedAssetId
import org.stellar.walletsdk.horizon.SigningKeyPair
import org.stellar.walletsdk.horizon.sign

class Sep6End2EndTest(val config: TestConfig, val jwt: String) {
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
        requestTimeoutMillis = 300000
        connectTimeoutMillis = 300000
        socketTimeoutMillis = 300000
      }
    }
  private val maxTries = 30

  private fun `test typical deposit end-to-end flow`() = runBlocking {
    val token = anchor.auth().authenticate(keypair)
    // TODO: migrate this to wallet-sdk when it's available
    val sep6Client = Sep6Client("${config.env["anchor.domain"]}/sep6", token.token)

    // Create a customer before starting the transaction
    anchor.customer(token).add(mapOf("first_name" to "John", "last_name" to "Doe"))

    val deposit =
      sep6Client.deposit(
        mapOf(
          "asset_code" to "USDC",
          "account" to keypair.address,
          "amount" to "0.01",
          "type" to "bank_account"
        )
      )
    waitStatus(deposit.id, "pending_customer_info_update", sep6Client)

    // Supply missing KYC info to continue with the transaction
    anchor.customer(token).add(mapOf("email_address" to "customer@email.com"))
    waitStatus(deposit.id, "completed", sep6Client)

    val completedDepositTxn = sep6Client.getTransaction(mapOf("id" to deposit.id))
    assertEquals(
      mapOf(
        "organization.bank_number" to
          InstructionField.builder()
            .value("121122676")
            .description("US Bank routing number")
            .build(),
        "organization.bank_account_number" to
          InstructionField.builder()
            .value("13719713158835300")
            .description("US Bank account number")
            .build()
      ),
      completedDepositTxn.transaction.instructions
    )
    val transactionByStellarId: GetTransactionResponse =
      sep6Client.getTransaction(
        mapOf("stellar_transaction_id" to completedDepositTxn.transaction.stellarTransactionId)
      )
    assertEquals(completedDepositTxn.transaction.id, transactionByStellarId.transaction.id)
  }

  private fun `test typical withdraw end-to-end flow`() = runBlocking {
    val token = anchor.auth().authenticate(keypair)
    // TODO: migrate this to wallet-sdk when it's available
    val sep6Client = Sep6Client("${config.env["anchor.domain"]}/sep6", token.token)

    // Create a customer before starting the transaction
    anchor.customer(token).add(mapOf("first_name" to "John", "last_name" to "Doe"))

    val withdraw =
      sep6Client.withdraw(
        mapOf("asset_code" to "USDC", "amount" to "0.01", "type" to "bank_account")
      )
    waitStatus(withdraw.id, "pending_customer_info_update", sep6Client)
    val withdrawTxn = sep6Client.getTransaction(mapOf("id" to withdraw.id))

    anchor
      .customer(token)
      .add(
        mapOf(
          "bank_account_type" to "checking",
          "bank_account_number" to "121122676",
          "bank_number" to "13719713158835300",
        )
      )
    waitStatus(withdraw.id, "pending_user_transfer_start", sep6Client)
    val transfer =
      wallet
        .stellar()
        .transaction(keypair, memo = Pair(MemoType.HASH, withdraw.memo))
        .transfer(
          withdraw.accountId,
          IssuedAssetId("USDC", "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"),
          "0.01"
        )
        .build()
    transfer.sign(keypair)
    wallet.stellar().submitTransaction(transfer)

    waitStatus(withdraw.id, "completed", sep6Client)
  }

  private suspend fun waitStatus(id: String, expectedStatus: String, sep6Client: Sep6Client) {
    for (i in 0..maxTries) {
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

  fun testAll() {
    `test typical deposit end-to-end flow`()
    `test typical withdraw end-to-end flow`()
  }
}
