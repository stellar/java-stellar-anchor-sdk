package org.stellar.reference.sep24

import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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

    if (cfg.sep24.rpcActionsEnabled) {
      sep24.rpcAction(
        "request_onchain_funds",
        buildJsonObject {
          put("transaction_id", JsonPrimitive(transactionId))
          put("message", JsonPrimitive("waiting on the user to transfer funds"))
          put(
            "amount_in",
            buildJsonObject {
              put("asset", JsonPrimitive(stellarAsset))
              put("amount", JsonPrimitive(amount.toPlainString()))
            }
          )
          put(
            "amount_out",
            buildJsonObject {
              put("asset", JsonPrimitive("iso4217:USD"))
              put("amount", JsonPrimitive(amount.subtract(fee).toPlainString()))
            }
          )
          put(
            "amount_fee",
            buildJsonObject {
              put("asset", JsonPrimitive(stellarAsset))
              put("amount", JsonPrimitive(fee.toPlainString()))
            }
          )
        }
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
    if (cfg.sep24.rpcActionsEnabled) {
      sep24.rpcAction(
        "notify_offchain_funds_sent",
        buildJsonObject {
          put("transaction_id", JsonPrimitive(transactionId))
          put("message", JsonPrimitive("pending external transfer"))
        }
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
    if (!cfg.sep24.rpcActionsEnabled) {
      sep24.patchTransaction(
        PatchTransactionTransaction(transactionId, "completed", message = "completed")
      )
    }
  }

  private suspend fun failTransaction(transactionId: String, message: String?) {
    if (cfg.sep24.rpcActionsEnabled) {
      sep24.rpcAction(
        "notify_transaction_error",
        buildJsonObject {
          put("transaction_id", JsonPrimitive(transactionId))
          put("message", JsonPrimitive(message))
        }
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
