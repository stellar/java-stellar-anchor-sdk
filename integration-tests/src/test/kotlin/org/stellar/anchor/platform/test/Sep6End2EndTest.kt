package org.stellar.anchor.platform.test

import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlin.test.DefaultAsserter.fail
import kotlin.test.assertContentEquals
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
import org.stellar.reference.client.AnchorReferenceServerClient
import org.stellar.reference.wallet.WalletServerClient
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
  private val anchorReferenceServerClient =
    AnchorReferenceServerClient(Url(config.env["reference.server.url"]!!))
  private val walletServerClient = WalletServerClient(Url(config.env["wallet.server.url"]!!))

  companion object {
    private val USDC =
      IssuedAssetId("USDC", "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP")
    private val basicInfoFields = listOf("first_name", "last_name", "email_address")
    private val customerInfo =
      mapOf(
        "first_name" to "John",
        "last_name" to "Doe",
        "address" to "123 Bay Street",
        "email_address" to "john@email.com",
        "id_type" to "drivers_license",
        "id_country_code" to "CAN",
        "id_issue_date" to "2023-01-01",
        "id_expiration_date" to "2028-01-01",
        "id_number" to "1234567890",
        "bank_account_number" to "13719713158835300",
        "bank_account_type" to "checking",
        "bank_number" to "123",
        "bank_branch_number" to "121122676"
      )
  }

  private fun `test typical deposit end-to-end flow`() = runBlocking {
    val token = anchor.auth().authenticate(keypair)
    // TODO: migrate this to wallet-sdk when it's available
    val sep6Client = Sep6Client("${config.env["anchor.domain"]}/sep6", token.token)

    // Create a customer before starting the transaction
    anchor.customer(token).add(basicInfoFields.associateWith { customerInfo[it]!! })

    val deposit =
      sep6Client.deposit(
        mapOf(
          "asset_code" to USDC.code,
          "account" to keypair.address,
          "amount" to "1",
          "type" to "SWIFT"
        )
      )
    waitStatus(deposit.id, "pending_customer_info_update", sep6Client)

    // Supply missing KYC info to continue with the transaction
    val additionalRequiredFields =
      sep6Client.getTransaction(mapOf("id" to deposit.id)).transaction.requiredCustomerInfoUpdates
    anchor.customer(token).add(additionalRequiredFields.associateWith { customerInfo[it]!! })
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

    val expectedStatuses = listOf("incomplete", "pending_customer_info_update", "completed")
    assertAnchorReceivedStatuses(deposit.id, expectedStatuses)
    assertWalletReceivedStatuses(deposit.id, expectedStatuses)
  }

  private fun `test typical withdraw end-to-end flow`() = runBlocking {
    val token = anchor.auth().authenticate(keypair)
    // TODO: migrate this to wallet-sdk when it's available
    val sep6Client = Sep6Client("${config.env["anchor.domain"]}/sep6", token.token)

    // Create a customer before starting the transaction
    anchor.customer(token).add(basicInfoFields.associateWith { customerInfo[it]!! })

    val withdraw =
      sep6Client.withdraw(
        mapOf("asset_code" to USDC.code, "amount" to "1", "type" to "bank_account")
      )
    waitStatus(withdraw.id, "pending_customer_info_update", sep6Client)

    // Supply missing financial account info to continue with the transaction
    val additionalRequiredFields =
      sep6Client.getTransaction(mapOf("id" to withdraw.id)).transaction.requiredCustomerInfoUpdates
    anchor.customer(token).add(additionalRequiredFields.associateWith { customerInfo[it]!! })
    waitStatus(withdraw.id, "pending_user_transfer_start", sep6Client)

    // Transfer the withdrawal amount to the Anchor
    val transfer =
      wallet
        .stellar()
        .transaction(keypair, memo = Pair(MemoType.HASH, withdraw.memo))
        .transfer(withdraw.accountId, USDC, "1")
        .build()
    transfer.sign(keypair)
    wallet.stellar().submitTransaction(transfer)
    waitStatus(withdraw.id, "completed", sep6Client)

    val expectedStatuses =
      listOf(
        "incomplete",
        "pending_customer_info_update",
        "pending_user_transfer_start",
        "pending_anchor",
        "completed"
      )
    assertAnchorReceivedStatuses(withdraw.id, expectedStatuses)
    assertWalletReceivedStatuses(withdraw.id, expectedStatuses)
  }

  private suspend fun assertAnchorReceivedStatuses(txnId: String, expected: List<String>) {
    val events = anchorReferenceServerClient.pollEvents(txnId, expected.size)
    val statuses = events.map { it.payload.transaction?.status.toString() }
    assertContentEquals(expected, statuses)
  }

  private suspend fun assertWalletReceivedStatuses(txnId: String, expected: List<String>) {
    val callbacks =
      walletServerClient.pollCallbacks(txnId, expected.size, GetTransactionResponse::class.java)
    val statuses = callbacks.map { it.transaction.status }
    assertContentEquals(expected, statuses)
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
