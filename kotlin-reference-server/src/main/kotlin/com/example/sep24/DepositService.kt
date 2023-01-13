package com.example.sep24

import com.example.data.Amount
import com.example.data.PatchTransactionRecord
import com.example.sep24.Sep24Util.getTransaction
import com.example.sep24.Sep24Util.patchTransaction
import com.example.sep24.Sep24Util.sendStellarTransaction
import java.math.BigDecimal
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class DepositService() {
  suspend fun getClientInfo(transactionId: String) {
    // 1. Gather all information from the client here, such as KYC.
    // In this simple implementation we do not require any additional input from the user.
  }

  suspend fun processDeposit(
    transactionId: String,
    amount: BigDecimal,
    account: String,
    assetCode: String,
    assetIssuer: String,
    memo: String?,
    memoType: String?
  ) {
    try {
      var transaction = getTransaction(transactionId)
      log.info { "Transaction found $transaction" }

      // 2. Wait for user to submit a transfer (e.g. Bank transfer)
      initiateTransfer(transactionId, amount)

      transaction = getTransaction(transactionId)
      log.info { "Transaction status changed: $transaction" }

      // 4. Notify user transaction is being processed
      patchTransaction(transactionId, "pending_anchor")

      transaction = getTransaction(transactionId)
      log.info { "Transaction status changed: $transaction" }

      // 5. Sign and send transaction
      val txHash = sendStellarTransaction(account, assetCode, assetIssuer, amount, memo, memoType)

      // 6. Finalize anchor transaction
      finalize(transactionId, txHash)

      log.info { "Transaction completed: $transactionId" }
    } catch (e: Exception) {
      log.error(e) { "Error happened during processing transaction $transactionId" }

      try {
        // If some error happens during the job, set anchor transaction to error status
        patchTransaction(transactionId, "error", e.message)
      } catch (e: Exception) {
        log.error(e) { "CRITICAL: failed to set transaction status to error" }
      }
    }
  }

  private suspend fun initiateTransfer(transactionId: String, amount: BigDecimal) {
    val fee = calculateFee(amount)

    patchTransaction(
      PatchTransactionRecord(
        transactionId,
        status = "pending_user_transfer_start",
        message = "waiting on the user to transfer funds",
        amountIn = Amount(amount.toPlainString()),
        amountOut = Amount(amount.subtract(fee).toPlainString()),
        amountFee = Amount(fee.toPlainString())
      )
    )
  }

  // Set 10% fee
  private fun calculateFee(amount: BigDecimal): BigDecimal {
    return amount.multiply(BigDecimal.valueOf(0.1))
  }

  private suspend fun finalize(transactionId: String, stellarTransactionId: String) {
    patchTransaction(
      PatchTransactionRecord(
        transactionId,
        "completed",
        message = "completed",
        stellarTransactionId = stellarTransactionId
      )
    )
  }
}
