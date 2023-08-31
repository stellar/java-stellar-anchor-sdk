package org.stellar.reference.sep24

import java.math.BigDecimal
import java.math.RoundingMode
import mu.KotlinLogging
import org.stellar.reference.data.*
import org.stellar.reference.service.SepHelper

private val log = KotlinLogging.logger {}

class WithdrawalService(private val cfg: Config) {
  private val sep24 = SepHelper(cfg)

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
      sep24.waitStellarTransaction(transactionId, "pending_anchor")

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
        failTransaction(transactionId, e.message)
      } catch (e: Exception) {
        log.error(e) { "CRITICAL: failed to set transaction status to error" }
      }
    }
  }

  private suspend fun initiateTransfer(transactionId: String, amount: BigDecimal, asset: String) {
    val fee = calculateFee(amount)
    val stellarAsset = "stellar:$asset"

    if (cfg.appSettings.rpcEnabled) {
      sep24.rpcAction(
        "request_onchain_funds",
        RequestOnchainFundsRequest(
          transactionId = transactionId,
          message = "waiting on the user to transfer funds",
          amountIn = AmountAssetRequest(asset = stellarAsset, amount = amount.toPlainString()),
          amountOut =
            AmountAssetRequest(
              asset = "iso4217:USD",
              amount = amount.subtract(fee).toPlainString()
            ),
          amountFee = AmountAssetRequest(asset = stellarAsset, amount = fee.toPlainString())
        )
      )
    } else {
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
  }

  private suspend fun sendExternal(transactionId: String) {
    if (cfg.appSettings.rpcEnabled) {
      sep24.rpcAction(
        "notify_offchain_funds_sent",
        NotifyOffchainFundsSentRequest(
          transactionId = transactionId,
          message = "pending external transfer"
        )
      )
    } else {
      sep24.patchTransaction(
        PatchTransactionTransaction(
          transactionId,
          "pending_external",
          message = "pending external transfer",
        )
      )

      // Send bank transfer, etc. here
    }
  }

  private suspend fun finalize(transactionId: String) {
    if (!cfg.appSettings.rpcEnabled) {
      sep24.patchTransaction(
        PatchTransactionTransaction(transactionId, "completed", message = "completed")
      )
    }
  }

  private suspend fun failTransaction(transactionId: String, message: String?) {
    if (cfg.appSettings.rpcEnabled) {
      sep24.rpcAction(
        "notify_transaction_error",
        NotifyTransactionErrorRequest(transactionId = transactionId, message = message)
      )
    } else {
      sep24.patchTransaction(transactionId, "error", message)
    }
  }

  // Set 10% fee
  private fun calculateFee(amount: BigDecimal): BigDecimal {
    val fee = amount.multiply(BigDecimal.valueOf(0.1))
    val scale = if (amount.scale() == 0) 1 else amount.scale()
    return fee.setScale(scale, RoundingMode.DOWN)
  }
}
