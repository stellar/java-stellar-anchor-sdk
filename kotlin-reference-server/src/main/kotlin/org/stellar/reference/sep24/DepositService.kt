package org.stellar.reference.sep24

import java.math.BigDecimal
import mu.KotlinLogging
import org.stellar.reference.data.*
import org.stellar.sdk.responses.operations.PaymentOperationResponse

private val log = KotlinLogging.logger {}

class DepositService(cfg: Config) {
  val sep24 = Sep24Helper(cfg)

  suspend fun processDeposit(
    transactionId: String,
    amount: BigDecimal,
    account: String,
    asset: String,
    memo: String?,
    memoType: String?
  ) {
    try {
      var transaction = sep24.getTransaction(transactionId)
      log.info { "Transaction found $transaction" }

      // 2. Wait for user to submit a transfer (e.g. Bank transfer)
      initiateTransfer(transactionId, amount, asset)

      transaction = sep24.getTransaction(transactionId)
      log.info { "Transaction status changed: $transaction" }

      // 4. Notify user transaction is being processed
      sep24.patchTransaction(
        transactionId,
        "pending_anchor",
        "funds received, transaction is being processed"
      )

      transaction = sep24.getTransaction(transactionId)
      log.info { "Transaction status changed: $transaction" }

      // 5. Sign and send transaction
      val txHash = sep24.sendStellarTransaction(account, asset, amount, memo, memoType)

      // 6. Finalize anchor transaction
      finalize(transactionId, txHash, asset, amount)

      log.info { "Transaction completed: $transactionId" }
    } catch (e: Exception) {
      log.error(e) { "Error happened during processing transaction $transactionId" }

      try {
        // If some error happens during the job, set anchor transaction to error status
        sep24.patchTransaction(transactionId, "error", e.message)
      } catch (e: Exception) {
        log.error(e) { "CRITICAL: failed to set transaction status to error" }
      }
    }
  }

  private suspend fun initiateTransfer(transactionId: String, amount: BigDecimal, asset: String) {
    val fee = calculateFee(amount)
    val stellarAsset = "stellar:$asset"

    sep24.patchTransaction(
      PatchTransactionTransaction(
        transactionId,
        status = "pending_user_transfer_start",
        message = "waiting on the user to transfer funds",
        amountIn = Amount(amount.toPlainString(), stellarAsset),
        amountOut = Amount(amount.subtract(fee).toPlainString(), stellarAsset),
        amountFee = Amount(fee.toPlainString(), stellarAsset)
      )
    )
  }

  // Set 10% fee
  private fun calculateFee(amount: BigDecimal): BigDecimal {
    return amount.multiply(BigDecimal.valueOf(0.1))
  }

  private suspend fun finalize(
    transactionId: String,
    stellarTransactionId: String,
    asset: String,
    amount: BigDecimal
  ) {
    val operationId: Long =
      sep24.server
        .operations()
        .forTransaction(stellarTransactionId)
        .execute()
        .records
        .filterIsInstance<PaymentOperationResponse>()
        .first()
        .id

    sep24.patchTransaction(
      PatchTransactionTransaction(
        transactionId,
        "completed",
        message = "completed",
        stellarTransactions =
          listOf(
            StellarTransaction(
              stellarTransactionId,
              payments =
                listOf(
                  StellarPayment(id = operationId.toString(), Amount(amount.toPlainString(), asset))
                )
            )
          )
      )
    )
  }
}
