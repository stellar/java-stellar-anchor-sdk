package org.stellar.reference.service.sep31

import mu.KotlinLogging
import org.stellar.reference.data.Config
import org.stellar.reference.data.NotifyOffchainFundsSentRequest
import org.stellar.reference.data.NotifyTransactionErrorRequest
import org.stellar.reference.service.SepHelper

private val log = KotlinLogging.logger {}

class ReceiveService(cfg: Config) {
  private val sepHelper = SepHelper(cfg)

  suspend fun processReceive(transactionId: String) {
    try {
      val transaction = sepHelper.getTransaction(transactionId)
      log.info { "Transaction found $transaction" }

      // 1. Wait for stellar transaction
      sepHelper.waitStellarTransaction(transactionId, "pending_receiver")

      // 2. Submit a transfer (e.g. Bank transfer)
      sendExternal(transactionId)

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

  private suspend fun sendExternal(transactionId: String) {
    sepHelper.rpcAction(
      "notify_offchain_funds_sent",
      NotifyOffchainFundsSentRequest(
        transactionId = transactionId,
        message = "external transfer sent"
      )
    )
  }

  private suspend fun failTransaction(transactionId: String, message: String?) {
    sepHelper.rpcAction(
      "notify_transaction_error",
      NotifyTransactionErrorRequest(transactionId = transactionId, message = message)
    )
  }
}
