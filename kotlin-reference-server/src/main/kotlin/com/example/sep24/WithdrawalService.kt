package com.example.sep24

import com.example.data.Amount
import com.example.data.Config
import com.example.data.PatchTransactionRecord
import java.math.BigDecimal
import mu.KotlinLogging
import org.stellar.sdk.responses.TransactionResponse

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
  ) {
    try {
      var transaction = sep24.getTransaction(transactionId)
      log.info { "Transaction found $transaction" }

      // 2. Wait for user to submit a stellar transfer
      val memo = initiateTransfer(transactionId, amount)

      transaction = sep24.getTransaction(transactionId)
      log.info { "Transaction status changed: $transaction" }

      // 3. Wait for stellar transaction
      val stellarTransaction = sep24.waitStellarTransaction(memo)

      validateTransaction(stellarTransaction)

      // 4. Send external funds
      sendExternal(transactionId, stellarTransaction.hash)

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

  private suspend fun initiateTransfer(transactionId: String, amount: BigDecimal): String {
    val fee = calculateFee(amount)
    val memo = transactionId.substring(0, 26)

    sep24.patchTransaction(
      PatchTransactionRecord(
        transactionId,
        status = "pending_user_transfer_start",
        message = "waiting on the user to transfer funds",
        amountIn = Amount(amount.toPlainString()),
        amountOut = Amount(amount.subtract(fee).toPlainString()),
        amountFee = Amount(fee.toPlainString()),
        memo = memo,
        memoType = "text",
        withdrawalAnchorAccount = cfg.secret.keyPair.accountId
      )
    )

    return memo
  }

  // Set 10% fee
  private fun calculateFee(amount: BigDecimal): BigDecimal {
    return amount.multiply(BigDecimal.valueOf(0.1))
  }

  private suspend fun sendExternal(transactionId: String, stellarTransactionId: String) {
    sep24.patchTransaction(
      PatchTransactionRecord(
        transactionId,
        "pending_external",
        message = "pending external transfer",
        stellarTransactionId = stellarTransactionId
      )
    )

    // Send bank transfer, etc. here
  }

  private suspend fun finalize(transactionId: String) {
    sep24.patchTransaction(
      PatchTransactionRecord(transactionId, "completed", message = "completed")
    )
  }

  private suspend fun validateTransaction(transaction: TransactionResponse) {
    // Validate user sent a valid transaction here
  }
}
