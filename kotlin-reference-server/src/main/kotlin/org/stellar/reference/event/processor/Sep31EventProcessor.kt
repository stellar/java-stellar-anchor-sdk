package org.stellar.reference.event.processor

import kotlinx.coroutines.runBlocking
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.platform.*
import org.stellar.anchor.api.rpc.method.RpcMethod
import org.stellar.anchor.api.sep.SepTransactionStatus.*
import org.stellar.reference.client.PlatformClient
import org.stellar.reference.data.*
import org.stellar.reference.di.ServiceContainer.customerService
import org.stellar.reference.di.ServiceContainer.sepHelper
import org.stellar.reference.log

class Sep31EventProcessor(
  private val platformClient: PlatformClient,
) : SepAnchorEventProcessor {
  companion object {
    val requiredKyc =
      listOf("bank_account_number", "bank_account_type", "bank_number", "bank_branch_number")
  }

  override suspend fun onQuoteCreated(event: SendEventRequest) {
    TODO("Not yet implemented")
  }

  override suspend fun onTransactionCreated(event: SendEventRequest) {
    log.warn { "Sep31EventProcessor onTransactionCreated" }
  }

  override suspend fun onTransactionStatusChanged(event: SendEventRequest) {
    log.warn { "Sep31EventProcessor onTransactionStatusChanged" }
    val transaction = event.payload.transaction!!
    val abc = transaction.status
    log.warn { "Sep31EventProcessor onTransactionStatusChanged $abc" }
    when (val status = transaction.status) {
      PENDING_RECEIVER -> {
        if (verifyKyc(transaction).isNotEmpty()) {
          requestKyc(event)
          return
        }
        sendExternal(transaction.id)
      }
      PENDING_EXTERNAL ->
        sepHelper.rpcAction(
          RpcMethod.NOTIFY_OFFCHAIN_FUNDS_SENT.toString(),
          NotifyOffchainFundsSentRequest(
            transactionId = transaction.id,
            message = "Funds sent to receiver",
          ),
        )
      COMPLETED -> {
        log.info { "Sep31EventProcessor - Transaction ${transaction.id} is completed" }
      }
      else -> {
        log.warn {
          "Sep31EventProcessor - Received transaction status changed event with unsupported status: $status"
        }
      }
    }
  }

  override suspend fun onCustomerUpdated(event: SendEventRequest) {
    log.warn { "Sep31EventProcessor onCustomerUpdated" }
    platformClient
      .getTransactions(
        GetTransactionsRequest.builder()
          .sep(TransactionsSeps.SEP_31)
          .orderBy(TransactionsOrderBy.CREATED_AT)
          .order(TransactionsOrder.ASC)
          .statuses(listOf(PENDING_CUSTOMER_INFO_UPDATE))
          .build()
      )
      .records
      .forEach { notifyCustomerUpdated(it) }
  }

  private fun notifyCustomerUpdated(transaction: GetTransactionResponse) {
    runBlocking {
      sepHelper.rpcAction(
        RpcMethod.NOTIFY_CUSTOMER_INFO_UPDATED.toString(),
        NotifyCustomerInfoUpdatedRequest(
          transactionId = transaction.id,
          message = "Customer info updated",
        ),
      )
    }
  }

  private fun verifyKyc(transaction: GetTransactionResponse): List<String> {
    val receiver = runBlocking {
      customerService.getCustomer(
        GetCustomerRequest.builder().transactionId(transaction.id).type("sep31-receiver").build()
      )
    }
    val providedFields = receiver.providedFields.keys
    return requiredKyc.filter { !providedFields.contains(it) }
  }
  private fun requestKyc(event: SendEventRequest) {
    runBlocking {
      sepHelper.rpcAction(
        RpcMethod.REQUEST_CUSTOMER_INFO_UPDATE.toString(),
        RequestCustomerInfoUpdateRequest(
          transactionId = event.payload.transaction!!.id,
          message = "Please update your info",
        ),
      )
    }
  }

  private suspend fun sendExternal(transactionId: String) {
    runBlocking {
      sepHelper.rpcAction(
        "notify_offchain_funds_sent",
        NotifyOffchainFundsSentRequest(
          transactionId = transactionId,
          message = "external transfer sent"
        )
      )
    }
  }
}
