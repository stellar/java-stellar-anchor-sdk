package org.stellar.anchor.platform.test

import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLProtocol
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.platform.CLIENT_WALLET_SECRET
import org.stellar.anchor.platform.Sep31Client
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.MemoHelper.convertBase64ToHex
import org.stellar.anchor.util.Sep1Helper
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.anchor.MemoType
import org.stellar.walletsdk.anchor.TransactionStatus.COMPLETED
import org.stellar.walletsdk.asset.IssuedAssetId
import org.stellar.walletsdk.asset.StellarAssetId
import org.stellar.walletsdk.horizon.SigningKeyPair
import org.stellar.walletsdk.horizon.sign

class Sep31End2EndTests(
  private val config: TestConfig,
  private val toml: Sep1Helper.TomlContent,
) {
  private val walletSecretKey = System.getenv("WALLET_SECRET_KEY") ?: CLIENT_WALLET_SECRET
  private val keypair = SigningKeyPair.fromSecret(walletSecretKey)
  private val wallet =
    Wallet(
      StellarConfiguration.Testnet,
      ApplicationConfiguration { defaultRequest { url { protocol = URLProtocol.HTTP } } }
    )
  private val usdc =
    IssuedAssetId("USDC", "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP")
  private val asset = usdc
  private val anchor = wallet.anchor(config.env["anchor.domain"]!!)
  private val maxTries = 40

  private fun `typical send asset end-to-end flow`() = runBlocking {
    val token = anchor.sep10().authenticate(keypair)
    val customer = anchor.sep12(token)
    val sep31Client = Sep31Client(toml.getString("DIRECT_PAYMENT_SERVER"), token.toString())

    // Create customers
    val sendingClientPayload =
      mapOf("first_name" to "Allie", "last_name" to "Grater", "email_address" to "allie@email.com")
    val sender = customer.add(sendingClientPayload)

    val receivingClientPayload =
      mapOf(
        "first_name" to "John",
        "last_name" to "Doe",
        "address" to "123 Washington Street",
        "city" to "San Francisco",
        "state_or_province" to "CA",
        "address_country_code" to "US",
        "clabe_number" to "1234",
        "bank_number" to "abcd",
        "bank_account_number" to "1234",
        "bank_account_type" to "checking"
      )
    val receiver = customer.add(receivingClientPayload)

    // Create SEP-31 transaction
    val amount = "10.0"

    val sep31PostTransactionRequest = Sep31PostTransactionRequest()
    sep31PostTransactionRequest.senderId = sender.id
    sep31PostTransactionRequest.receiverId = receiver.id
    sep31PostTransactionRequest.amount = amount
    sep31PostTransactionRequest.assetCode = asset.code
    sep31PostTransactionRequest.assetIssuer = asset.issuer
    sep31PostTransactionRequest.fields =
      Sep31PostTransactionRequest.Sep31TxnFields(
        hashMapOf(
          "receiver_routing_number" to "r0123",
          "receiver_account_number" to "a0456",
          "type" to "SWIFT"
        )
      )
    val transaction = sep31Client.postTransaction(sep31PostTransactionRequest)

    // Check transaction status
    val sep31GetTransactionResponse = sep31Client.getTransaction(transaction.id)
    assertEquals("pending_sender", sep31GetTransactionResponse.transaction.status)

    // Set parameters to send a transfer transaction
    val memo = Pair(MemoType.HASH, convertBase64ToHex(transaction.stellarMemo))

    // Submit transfer transaction
    sendAsset(asset, memo, amount, transaction.stellarAccountId)

    // Wait for the status to change to COMPLETED
    waitStatus(transaction.id, COMPLETED.toString(), sep31Client)
  }

  private suspend fun sendAsset(
    asset: StellarAssetId,
    memo: Pair<MemoType, String>,
    amount: String,
    receiverAccountId: String
  ) {
    val transfer =
      wallet
        .stellar()
        .transaction(sourceAddress = keypair, memo = memo)
        .transfer(receiverAccountId, asset, amount)
        .build()

    transfer.sign(keypair)
    wallet.stellar().submitTransaction(transfer)
  }

  private suspend fun waitStatus(id: String, expectedStatus: String, sep31Client: Sep31Client) {
    var status: String? = null

    for (i in 0..maxTries) {
      // Get transaction info
      val sep31GetTransactionResponse = sep31Client.getTransaction(id)

      if (status != sep31GetTransactionResponse.transaction.status) {
        status = sep31GetTransactionResponse.transaction.status
        println("Deposit transaction status changed to $status.")
      }

      delay(1.seconds)

      if (sep31GetTransactionResponse.transaction.status == expectedStatus) {
        return
      }
    }

    fail("Transaction wasn't $expectedStatus in $maxTries tries, last status: $status")
  }

  fun testAll() {
    println("Running SEP-31 end-to-end tests...")
    `typical send asset end-to-end flow`()
  }
}
