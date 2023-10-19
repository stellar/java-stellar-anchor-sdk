package org.stellar.reference.event.processor

import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.api.platform.*
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.InstructionField
import org.stellar.anchor.api.shared.StellarPayment
import org.stellar.anchor.api.shared.StellarTransaction
import org.stellar.reference.callbacks.customer.CustomerService
import org.stellar.reference.client.PlatformClient
import org.stellar.reference.data.Config
import org.stellar.reference.log
import org.stellar.sdk.*

class Sep6EventProcessor(
  private val config: Config,
  private val server: Server,
  private val platformClient: PlatformClient,
  private val customerService: CustomerService,
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
        if (event.transaction.status != SepTransactionStatus.INCOMPLETE) {
          log.warn(
            "Received $kind transaction created event with unsupported status: ${event.transaction.status}"
          )
          return
        }
        val missingFields = verifyKyc(event.transaction.customers.sender.id, kind)
        val (status, requiredFields) =
          if (missingFields.isEmpty()) {
            SepTransactionStatus.PENDING_ANCHOR to null
          } else {
            SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE to missingFields
          }
        runBlocking {
          patchTransaction(
            PlatformTransactionData.builder()
              .id(event.transaction.id)
              .status(status)
              .updatedAt(Instant.now())
              .requiredCustomerInfoUpdates(requiredFields)
              .build()
          )
        }
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
      SepTransactionStatus.PENDING_ANCHOR -> {
        val missingFields = verifyKyc(event.transaction.customers.sender.id, Kind.DEPOSIT)
        val (newStatus, requiredFields) =
          if (missingFields.isEmpty()) {
            SepTransactionStatus.COMPLETED to null
          } else {
            SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE to missingFields
          }
        runBlocking {
          patchTransaction(
            PlatformTransactionData.builder()
              .id(event.transaction.id)
              .status(newStatus)
              .updatedAt(Instant.now())
              .requiredCustomerInfoUpdates(requiredFields)
              .apply {
                if (newStatus == SepTransactionStatus.COMPLETED) {
                  completedAt(Instant.now())
                  requiredInfoMessage(null)
                  requiredInfoUpdates(null)
                  requiredCustomerInfoMessage(null)
                  requiredCustomerInfoUpdates(null)
                }
              }
              .build()
          )
        }
      }
      SepTransactionStatus.COMPLETED -> {
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
      // The transaction only moves into this state after the user has submitted funds to the
      // Anchor.
      SepTransactionStatus.PENDING_ANCHOR -> {
        runBlocking {
          patchTransaction(
            PlatformTransactionData.builder()
              .id(transaction.id)
              .status(SepTransactionStatus.COMPLETED)
              .updatedAt(Instant.now())
              .completedAt(Instant.now())
              .build()
          )
        }
      }
      SepTransactionStatus.COMPLETED -> {
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
              .statuses(listOf(SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE))
              .build()
          )
          .records
      }
      .forEach { transaction ->
        when (transaction.kind) {
          Kind.DEPOSIT -> handleDepositTransaction(transaction)
          Kind.WITHDRAWAL -> handleWithdrawTransaction(transaction)
          else -> {
            log.warn(
              "Received transaction created event with unsupported kind: ${transaction.kind}"
            )
          }
        }
      }
  }

  private fun handleDepositTransaction(transaction: GetTransactionResponse) {
    if (verifyKyc(transaction.customers.sender.id, Kind.DEPOSIT).isNotEmpty()) {
      return
    }

    val keypair = KeyPair.fromSecretSeed(config.appSettings.secret)
    val assetCode = transaction.amountExpected.asset.toAssetId()

    val asset = Asset.create(assetCode)
    val amount = transaction.amountExpected.amount
    val destination = transaction.destinationAccount

    val stellarTxn = submitStellarTransaction(keypair.accountId, destination, asset, amount)
    runBlocking {
      patchTransaction(
        PlatformTransactionData.builder()
          .id(transaction.id)
          .status(SepTransactionStatus.COMPLETED)
          .updatedAt(Instant.now())
          .completedAt(Instant.now())
          .requiredInfoMessage(null)
          .requiredInfoUpdates(null)
          .requiredCustomerInfoMessage(null)
          .requiredCustomerInfoUpdates(null)
          .instructions(
            mapOf(
              "organization.bank_number" to
                InstructionField.builder()
                  .value("121122676")
                  .description("US Bank routing number")
                  .build(),
              "organization.bank_account_number" to
                InstructionField.builder()
                  .value("13719713158835300")
                  .description("US Bank account number")
                  .build(),
            )
          )
          .stellarTransactions(listOf(stellarTxn))
          .build()
      )
    }
  }

  private fun handleWithdrawTransaction(transaction: GetTransactionResponse) {
    if (verifyKyc(transaction.customers.sender.id, Kind.WITHDRAWAL).isNotEmpty()) {
      return
    }

    runBlocking {
      patchTransaction(
        PlatformTransactionData.builder()
          .id(transaction.id)
          .status(SepTransactionStatus.PENDING_USR_TRANSFER_START)
          .updatedAt(Instant.now())
          .requiredInfoMessage(null)
          .requiredInfoUpdates(null)
          .requiredCustomerInfoUpdates(null)
          .requiredCustomerInfoUpdates(null)
          .build()
      )
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

  private fun verifyKyc(customerId: String, kind: Kind): List<String> {
    val customer = customerService.getCustomer(GetCustomerRequest.builder().id(customerId).build())
    val providedFields = customer.providedFields.keys
    return requiredKyc
      .plus(if (kind == Kind.DEPOSIT) depositRequiredKyc else withdrawRequiredKyc)
      .filter { !providedFields.contains(it) }
  }

  private fun submitStellarTransaction(
    source: String,
    destination: String,
    asset: Asset,
    amount: String
  ): StellarTransaction {
    // TODO: use Kotlin wallet SDK
    val account = server.accounts().account(source)
    val transaction =
      TransactionBuilder(account, Network.TESTNET)
        .setBaseFee(100)
        .setTimeout(60L)
        .addOperation(PaymentOperation.Builder(destination, asset, amount).build())
        .build()
    transaction.sign(KeyPair.fromSecretSeed(config.appSettings.secret))
    val txnResponse = server.submitTransaction(transaction)
    if (!txnResponse.isSuccess) {
      throw RuntimeException("Error submitting transaction: ${txnResponse.extras.resultCodes}")
    }
    val txHash = txnResponse.hash
    val operationId = server.operations().forTransaction(txHash).execute().records.firstOrNull()?.id
    val stellarPayment =
      StellarPayment(
        operationId.toString(),
        Amount(amount, asset.toString()),
        StellarPayment.Type.PAYMENT,
        source,
        destination
      )
    return StellarTransaction.builder().id(txHash).payments(listOf(stellarPayment)).build()
  }

  private suspend fun patchTransaction(data: PlatformTransactionData) {
    val request =
      PatchTransactionsRequest.builder()
        .records(listOf(PatchTransactionRequest.builder().transaction(data).build()))
        .build()
    platformClient.patchTransactions(request)
  }
}
