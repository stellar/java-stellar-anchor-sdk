package com.example.sep24

import com.example.data.*
import com.example.data.Transaction
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import java.math.BigDecimal
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.commons.codec.binary.Hex
import org.stellar.sdk.*

class Sep24Helper(private val cfg: Config) {
  private val log = KotlinLogging.logger {}

  val client = HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
  }

  val baseUrl = cfg.sep24.anchorPlatformUrl

  val server = Server(cfg.sep24.horizonUrl)

  internal suspend fun patchTransaction(patchRecord: PatchTransactionTransaction) {
    val resp =
      client.patch("$baseUrl/transactions") {
        contentType(ContentType.Application.Json)
        setBody(PatchTransactionsRequest(listOf(PatchTransactionRecord(patchRecord))))
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
    patchTransaction(PatchTransactionTransaction(transactionId, newStatus, message))
  }

  internal suspend fun getTransaction(transactionId: String): Transaction {
    return client.get("$baseUrl/transactions/$transactionId").body()
  }

  internal suspend fun sendStellarTransaction(
    destinationAddress: String,
    assetString: String,
    amount: BigDecimal,
    memo: String?,
    memoType: String?
  ): String {
    val myAccount = server.accounts().account(cfg.sep24.keyPair.accountId)
    val asset = Asset.create(assetString)

    val transactionBuilder =
      TransactionBuilder(myAccount, Network.TESTNET)
        .setBaseFee(100)
        .setTimeout(60)
        .addOperation(
          PaymentOperation.Builder(destinationAddress, asset, amount.toPlainString()).build()
        )

    if (memo != null && memoType != null) {
      transactionBuilder.addMemo(
        when (memoType) {
          "text" -> Memo.text(memo)
          "id" -> Memo.id(memo.toLong())
          "hash" -> Memo.hash(Hex.encodeHexString(Base64.getDecoder().decode(memo)))
          else -> throw Exception("Unsupported memo type")
        }
      )
    }

    val transaction = transactionBuilder.build()

    transaction.sign(cfg.sep24.keyPair)

    val resp = server.submitTransaction(transaction)

    if (!resp.isSuccess) {
      throw Exception(
        "Failed to submit transaction with code: ${resp?.extras?.resultCodes?.transactionResultCode}"
      )
    }

    return resp.hash
  }

  // Pulling status change from anchor. Alternatively, listen to AnchorEvent for transaction status
  // change
  internal suspend fun waitStellarTransaction(txId: String) {
    for (i in 1..(30 * 60 / 5)) {
      log.info { "Waiting for user to transfer funds" }

      val transaction = getTransaction(txId)

      if (transaction.status == "pending_anchor") {
        log.info { "User transfer was successful" }

        return
      } else {
        delay(5.seconds)
      }
    }

    throw Exception("Transaction hasn't been sent in 30 minutes, giving up")
  }

  internal fun validateTransaction(transaction: Transaction) {
    // Sum of payments (if multiple) sent by the user, filtered by requested asset
    val paymentSum =
      transaction.stellarTransactions!!
        .flatMap { it.payments }
        .filter { "stellar:${it.amount.asset}" == transaction.amountIn!!.asset }
        .map { it.amount.amount!!.toBigDecimal() }
        .reduce(BigDecimal::add)

    val amountIn = transaction.amountIn!!.amount!!.toBigDecimal()

    if (paymentSum < amountIn) {
      throw Exception(
        "Amount of payments received is not equal to amount requested. Deficit: ${(paymentSum - amountIn).stripTrailingZeros().toPlainString()})"
      )
    }
  }
}
