package org.stellar.reference.sep24

import java.math.BigDecimal
import mu.KotlinLogging
import org.stellar.reference.data.Amount
import org.stellar.reference.data.Config
import org.stellar.reference.data.PatchTransactionTransaction

private val log = KotlinLogging.logger {}

class WithdrawalService(private val cfg: Config) {
  private val sep24 = Sep24Helper(cfg)

  suspend fun getClientInfo(transactionId: String) {
    // 1. Gather all information from the client here, such as KYC.
    // In this simple implementation we do not require any additional input from the user.
  }

  suspend fun processWithdrawal(
    transactionId: String,
    amount: BigDecimal,
    asset: String,
  ) {
    try {
      var transaction = sep24.getTransaction(transactionId)
      log.info { "Transaction found $transaction" }

      // 2. Wait for user to submit a stellar transfer
      initiateTransfer(transactionId, amount, asset)

      transaction = sep24.getTransaction(transactionId)
      log.info { "Transaction status changed: $transaction" }

      // 3. Wait for stellar transaction
      sep24.waitStellarTransaction(transactionId)

      transaction = sep24.getTransaction(transactionId)
      log.info { "Transaction status changed: $transaction" }

      sep24.validateTransaction(transaction)

      // 4. Send external funds
      sendExternal(transactionId)

      // 5. Finalize anchor transaction
      finalize(transactionId)

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
        amountFee = Amount(fee.toPlainString(), stellarAsset),
      )
    )
  }

  // Set 10% fee
  private fun calculateFee(amount: BigDecimal): BigDecimal {
    return amount.multiply(BigDecimal.valueOf(0.1))
  }

  private suspend fun sendExternal(transactionId: String) {
    sep24.patchTransaction(
      PatchTransactionTransaction(
        transactionId,
        "pending_external",
        message = "pending external transfer",
      )
    )

    // Send bank transfer, etc. here
  }

  private suspend fun finalize(transactionId: String) {
    sep24.patchTransaction(
      PatchTransactionTransaction(transactionId, "completed", message = "completed")
    )
  }
}
