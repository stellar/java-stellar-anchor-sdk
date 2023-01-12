package com.example.sep24

import com.example.data.PatchTransactionRecord
import com.example.data.PatchTransactionsRequest
import com.example.data.Transaction
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.stellar.sdk.*
import org.stellar.sdk.responses.TransactionResponse

object Sep24Util {
  private val log = KotlinLogging.logger {}

  // TODO: make parameters in this class configurable
  private val myKey =
    System.getenv("STELLAR_KEY") ?: "SDYGC4TW5HHR5JA6CB2XLTTBF2DZRH2KDPBDPV3D5TXM6GF7FBPRZF3I"
  internal val myKeyPair = KeyPair.fromSecretSeed(myKey)

  val client = HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
  }

  val baseUrl = "http://localhost:8080"

  val server = Server("https://horizon-testnet.stellar.org")

  internal suspend fun patchTransaction(patchRecord: PatchTransactionRecord) {
    val resp =
      client.patch("$baseUrl/transactions") {
        contentType(ContentType.Application.Json)
        setBody(PatchTransactionsRequest(listOf(patchRecord)))
      }

    if (resp.status != HttpStatusCode.OK) {
      val respBody = resp.bodyAsText()

      log.error { "Unexpected status code on patch transaction. Response body: $respBody" }

      throw Exception(respBody)
    }
  }

  internal suspend fun patchTransaction(
    transactionId: String,
    newStatus: String,
    message: String? = null
  ) {
    patchTransaction(PatchTransactionRecord(transactionId, newStatus, message))
  }

  internal suspend fun getTransaction(transactionId: String): Transaction {
    return client.get("$baseUrl/transactions/$transactionId").body()
  }

  internal suspend fun sendStellarTransaction(
    destinationAddress: String,
    assetCode: String,
    assetIssuer: String,
    amount: BigDecimal
  ): String {
    val myAccount = server.accounts().account(myKeyPair.accountId)
    val asset = Asset.create(null, assetCode, assetIssuer)

    val transaction =
      TransactionBuilder(myAccount, Network.TESTNET)
        .setBaseFee(100)
        .setTimeout(60)
        .addOperation(
          PaymentOperation.Builder(destinationAddress, asset, amount.toPlainString()).build()
        )
        .build()

    transaction.sign(myKeyPair)

    val resp = server.submitTransaction(transaction)

    if (!resp.isSuccess) {
      throw Exception(
        "Failed to submit transaction with code: ${resp?.extras?.resultCodes?.transactionResultCode}"
      )
    }

    return resp.hash
  }

  internal suspend fun waitStellarTransaction(memo: String): TransactionResponse {
    for (i in 1..(30 * 60 / 5)) {
      val transactions =
        server.transactions().forAccount(myKeyPair.accountId).limit(200).execute().records

      val transaction =
        transactions
          .filter { it.memo != null }
          .filter { it.memo is MemoText }
          .firstOrNull { (it.memo as MemoText).text == memo }

      if (transaction != null) {
        return transaction
      } else {
        delay(5.seconds)
      }
    }

    throw Exception("Transaction hasn't been sent in 30 minutes, giving up")
  }
}
