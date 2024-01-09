package org.stellar.reference.event.processor

import java.time.Instant
import java.util.*
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.api.platform.*
import org.stellar.anchor.api.platform.PatchTransactionsRequest
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind
import org.stellar.anchor.api.rpc.method.RpcMethod
import org.stellar.anchor.api.sep.SepTransactionStatus.*
import org.stellar.reference.callbacks.customer.CustomerService
import org.stellar.reference.client.PlatformClient
import org.stellar.reference.data.*
import org.stellar.reference.log
import org.stellar.reference.service.SepHelper
import org.stellar.reference.transactionWithRetry
import org.stellar.sdk.*

class Sep6EventProcessor(
  private val config: Config,
  private val server: Server,
  private val platformClient: PlatformClient,
  private val customerService: CustomerService,
  private val sepHelper: SepHelper,
  /** Map of transaction ID to Stellar transaction ID. */
  private val onchainPayments: MutableMap<String, String> = mutableMapOf(),
  /** Map of transaction ID to external transaction ID. */
  private val offchainPayments: MutableMap<String, String> = mutableMapOf()
) : SepAnchorEventProcessor {
  companion object {
    val requiredKyc =
      listOf("id_type", "id_country_code", "id_issue_date", "id_expiration_date", "id_number")
    val depositRequiredKyc = listOf("address")
    val withdrawRequiredKyc =
      listOf("bank_account_number", "bank_account_type", "bank_number", "bank_branch_number")
  }

  override fun onQuoteCreated(event: AnchorEvent) {
    TODO("Not yet implemented")
  }

  override fun onTransactionCreated(event: AnchorEvent) {
    when (val kind = event.transaction.kind) {
      Kind.DEPOSIT,
      Kind.WITHDRAWAL -> {
        requestKyc(event)
        requestCustomerFunds(event.transaction)
      }
      else -> {
        log.warn("Received transaction created event with unsupported kind: $kind")
      }
    }
  }

  override fun onTransactionError(event: AnchorEvent) {
    log.warn("Received transaction error event: $event")
  }

  override fun onTransactionStatusChanged(event: AnchorEvent) {
    when (val kind = event.transaction.kind) {
      Kind.DEPOSIT -> onDepositTransactionStatusChanged(event)
      Kind.WITHDRAWAL -> onWithdrawTransactionStatusChanged(event)
      else -> {
        log.warn("Received transaction created event with unsupported kind: $kind")
      }
    }
  }

  private fun onDepositTransactionStatusChanged(event: AnchorEvent) {
    val transaction = event.transaction
    when (val status = transaction.status) {
      PENDING_ANCHOR -> {
        val customer = transaction.customers.sender
        if (verifyKyc(customer.account, customer.memo, Kind.DEPOSIT).isNotEmpty()) {
          requestKyc(event)
          return
        }
        runBlocking {
          val keypair = KeyPair.fromSecretSeed(config.appSettings.secret)
          lateinit var stellarTxnId: String
          if (config.appSettings.custodyEnabled) {
            sepHelper.rpcAction(
              RpcMethod.DO_STELLAR_PAYMENT.toString(),
              DoStellarPaymentRequest(transactionId = transaction.id)
            )
          } else {
            transactionWithRetry {
              stellarTxnId =
                submitStellarTransaction(
                  keypair.accountId,
                  transaction.destinationAccount,
                  Asset.create(transaction.amountExpected.asset.toAssetId()),
                  transaction.amountExpected.amount
                )
            }
            onchainPayments[transaction.id] = stellarTxnId
            patchTransaction(
              PlatformTransactionData.builder()
                .id(transaction.id)
                .status(PENDING_STELLAR)
                .updatedAt(Instant.now())
                .build()
            )
          }
        }
      }
      PENDING_USR_TRANSFER_START ->
        runBlocking {
          sepHelper.rpcAction(
            RpcMethod.NOTIFY_OFFCHAIN_FUNDS_RECEIVED.toString(),
            NotifyOffchainFundsReceivedRequest(
              transactionId = transaction.id,
              message = "Funds received from user",
            )
          )
        }
      PENDING_STELLAR ->
        runBlocking {
          sepHelper.rpcAction(
            RpcMethod.NOTIFY_ONCHAIN_FUNDS_SENT.toString(),
            NotifyOnchainFundsSentRequest(
              transactionId = transaction.id,
              message = "Funds sent to user",
              stellarTransactionId = onchainPayments[transaction.id]!!
            )
          )
        }
      COMPLETED -> {
        log.info("Transaction ${transaction.id} completed")
      }
      else -> {
        log.warn("Received transaction status changed event with unsupported status: $status")
      }
    }
  }

  private fun onWithdrawTransactionStatusChanged(event: AnchorEvent) {
    val transaction = event.transaction
    when (val status = transaction.status) {
      PENDING_ANCHOR -> {
        val customer = transaction.customers.sender
        if (verifyKyc(customer.account, customer.memo, Kind.WITHDRAWAL).isNotEmpty()) {
          requestKyc(event)
          return
        }
        runBlocking {
          if (offchainPayments[transaction.id] == null && transaction.transferReceivedAt != null) {
            // If the amount was not specified at transaction initialization, set the
            // amountOut and amountFee fields after receiving the onchain deposit.
            if (transaction.amountOut.amount.equals("0")) {
              sepHelper.rpcAction(
                RpcMethod.NOTIFY_AMOUNTS_UPDATED.toString(),
                NotifyAmountsUpdatedRequest(
                  transactionId = transaction.id,
                  amountOut = AmountRequest(amount = transaction.amountIn.amount),
                  amountFee = AmountRequest(amount = "0")
                )
              )
            }
            val externalTxnId = UUID.randomUUID()
            offchainPayments[transaction.id] = externalTxnId.toString()
            sepHelper.rpcAction(
              RpcMethod.NOTIFY_OFFCHAIN_FUNDS_PENDING.toString(),
              NotifyOffchainFundsPendingRequest(
                transactionId = transaction.id,
                message = "Funds sent to user",
                externalTransactionId = externalTxnId.toString()
              )
            )
          } else if (transaction.transferReceivedAt != null) {
            sepHelper.rpcAction(
              RpcMethod.NOTIFY_OFFCHAIN_FUNDS_AVAILABLE.toString(),
              NotifyOffchainFundsAvailableRequest(
                transactionId = transaction.id,
                message = "Funds available for withdrawal",
                externalTransactionId = offchainPayments[transaction.id]!!
              )
            )
          }
        }
      }
      PENDING_EXTERNAL ->
        runBlocking {
          sepHelper.rpcAction(
            RpcMethod.NOTIFY_OFFCHAIN_FUNDS_SENT.toString(),
            NotifyOffchainFundsSentRequest(
              transactionId = transaction.id,
              message = "Funds sent to user",
            )
          )
        }
      COMPLETED -> {
        log.info("Transaction ${transaction.id} completed")
      }
      else -> {
        log.warn("Received transaction status changed event with unsupported status: $status")
      }
    }
  }

  override fun onCustomerUpdated(event: AnchorEvent) {
    runBlocking {
        platformClient
          .getTransactions(
            GetTransactionsRequest.builder()
              .sep(TransactionsSeps.SEP_6)
              .orderBy(TransactionsOrderBy.CREATED_AT)
              .order(TransactionsOrder.ASC)
              .statuses(listOf(PENDING_CUSTOMER_INFO_UPDATE))
              .build()
          )
          .records
      }
      .forEach { requestCustomerFunds(it) }
  }

  private fun requestCustomerFunds(transaction: GetTransactionResponse) {
    val customer = transaction.customers.sender
    when (transaction.kind) {
      Kind.DEPOSIT -> {
        if (verifyKyc(customer.account, customer.memo, Kind.DEPOSIT).isEmpty()) {
          runBlocking {
            sepHelper.rpcAction(
              RpcMethod.REQUEST_OFFCHAIN_FUNDS.toString(),
              RequestOffchainFundsRequest(
                transactionId = transaction.id,
                message = "Please deposit the amount to the following bank account",
                amountIn =
                  AmountAssetRequest(
                    asset = "iso4217:USD",
                    amount = transaction.amountExpected.amount
                  ),
                amountOut =
                  AmountAssetRequest(
                    asset = transaction.amountExpected.asset,
                    amount = transaction.amountExpected.amount
                  ),
                amountFee = AmountAssetRequest(asset = "iso4217:USD", amount = "0"),
                instructions =
                  mapOf(
                    "organization.bank_number" to
                      InstructionField(value = "121122676", description = "US Bank routing number"),
                    "organization.bank_account_number" to
                      InstructionField(
                        value = "13719713158835300",
                        description = "US Bank account number"
                      ),
                  )
              )
            )
          }
        }
      }
      Kind.WITHDRAWAL -> {
        if (verifyKyc(customer.account, customer.memo, Kind.WITHDRAWAL).isEmpty()) {
          runBlocking {
            if (transaction.amountExpected.amount != null) {
              // The amount was specified at transaction initialization
              sepHelper.rpcAction(
                RpcMethod.REQUEST_ONCHAIN_FUNDS.toString(),
                RequestOnchainFundsRequest(
                  transactionId = transaction.id,
                  message = "Please deposit the amount to the following address",
                  amountIn =
                    AmountAssetRequest(
                      asset = transaction.amountExpected.asset,
                      amount = transaction.amountExpected.amount
                    ),
                  amountOut =
                    AmountAssetRequest(
                      asset = "iso4217:USD",
                      amount = transaction.amountExpected.amount
                    ),
                  amountFee =
                    AmountAssetRequest(asset = transaction.amountExpected.asset, amount = "0")
                )
              )
            } else {
              sepHelper.rpcAction(
                RpcMethod.REQUEST_ONCHAIN_FUNDS.toString(),
                RequestOnchainFundsRequest(
                  transactionId = transaction.id,
                  message = "Please deposit to the following address",
                  amountIn = AmountAssetRequest(transaction.amountExpected.asset, "0"),
                  amountOut = AmountAssetRequest("iso4217:USD", "0"),
                  amountFee = AmountAssetRequest(transaction.amountExpected.asset, "0")
                )
              )
            }
          }
        }
      }
      else -> {
        log.warn("Received transaction created event with unsupported kind: ${transaction.kind}")
      }
    }
  }

  private fun verifyKyc(sep10Account: String, sep10AccountMemo: String?, kind: Kind): List<String> {
    val customer =
      customerService.getCustomer(
        GetCustomerRequest.builder()
          .account(sep10Account)
          .memo(sep10AccountMemo)
          .memoType(if (sep10AccountMemo != null) "id" else null)
          .build()
      )
    val providedFields = customer.providedFields.keys
    return requiredKyc
      .plus(if (kind == Kind.DEPOSIT) depositRequiredKyc else withdrawRequiredKyc)
      .filter { !providedFields.contains(it) }
  }

  private fun requestKyc(event: AnchorEvent) {
    val kind = event.transaction.kind
    val customer = event.transaction.customers.sender
    val missingFields = verifyKyc(customer.account, customer.memo, kind)
    runBlocking {
      if (missingFields.isNotEmpty()) {
        sepHelper.rpcAction(
          RpcMethod.REQUEST_CUSTOMER_INFO_UPDATE.toString(),
          RequestCustomerInfoUpdateHandler(
            transactionId = event.transaction.id,
            message = "Please update your info",
            requiredCustomerInfoUpdates = missingFields
          )
        )
      }
    }
  }

  private fun String.toAssetId(): String {
    val parts = this.split(":")
    return when (parts.size) {
      3 -> "${parts[1]}:${parts[2]}"
      2 -> parts[1]
      else -> throw RuntimeException("Invalid asset format: $this")
    }
  }

  private fun submitStellarTransaction(
    source: String,
    destination: String,
    asset: Asset,
    amount: String
  ): String {
    // TODO: use Kotlin wallet SDK
    val account = server.accounts().account(source)
    val transaction =
      TransactionBuilder(account, Network.TESTNET)
        .setBaseFee(100)
        .addPreconditions(
          TransactionPreconditions.builder().timeBounds(TimeBounds.expiresAfter(60)).build()
        )
        .addOperation(PaymentOperation.Builder(destination, asset, amount).build())
        .build()
    transaction.sign(KeyPair.fromSecretSeed(config.appSettings.secret))
    val txnResponse = server.submitTransaction(transaction)
    if (!txnResponse.isSuccess) {
      throw RuntimeException("Error submitting transaction: ${txnResponse.extras.resultCodes}")
    }

    return txnResponse.hash
  }

  private suspend fun patchTransaction(data: PlatformTransactionData) {
    val request =
      PatchTransactionsRequest.builder()
        .records(listOf(PatchTransactionRequest.builder().transaction(data).build()))
        .build()
    platformClient.patchTransactions(request)
  }
}
