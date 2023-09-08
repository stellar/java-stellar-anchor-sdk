package org.stellar.reference.event.processor

import java.lang.RuntimeException
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.api.platform.PatchTransactionRequest
import org.stellar.anchor.api.platform.PatchTransactionsRequest
import org.stellar.anchor.api.platform.PlatformTransactionData
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.InstructionField
import org.stellar.anchor.api.shared.StellarPayment
import org.stellar.anchor.api.shared.StellarTransaction
import org.stellar.reference.callbacks.customer.CustomerService
import org.stellar.reference.client.PlatformClient
import org.stellar.reference.data.Config
import org.stellar.reference.log
import org.stellar.sdk.Asset
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Network
import org.stellar.sdk.PaymentOperation
import org.stellar.sdk.Server
import org.stellar.sdk.TransactionBuilder

class Sep6EventProcessor(
  private val config: Config,
  private val server: Server,
  private val platformClient: PlatformClient,
  private val customerService: CustomerService,
  private val activeTransactionStore: ActiveTransactionStore
) : SepAnchorEventProcessor {
  override fun onQuoteCreated(event: AnchorEvent) {
    TODO("Not yet implemented")
  }

  override fun onTransactionCreated(event: AnchorEvent) {
    when (val kind = event.transaction.kind) {
      PlatformTransactionData.Kind.DEPOSIT -> onDepositTransactionCreated(event)
      PlatformTransactionData.Kind.WITHDRAWAL -> TODO("Withdrawals not yet supported")
      else -> {
        log.warn("Received transaction created event with unsupported kind: $kind")
      }
    }
  }

  private fun onDepositTransactionCreated(event: AnchorEvent) {
    if (event.transaction.status != SepTransactionStatus.INCOMPLETE) {
      log.warn(
        "Received deposit transaction created event with unsupported status: ${event.transaction.status}"
      )
      return
    }
    val customer =
      customerService.getCustomer(
        GetCustomerRequest.builder().account(event.transaction.destinationAccount).build()
      )
    runBlocking {
      patchTransaction(
        PlatformTransactionData.builder()
          .id(event.transaction.id)
          .status(SepTransactionStatus.PENDING_ANCHOR)
          .build()
      )
    }
    activeTransactionStore.addTransaction(customer.id, event.transaction.id)
    log.info(
      "Added transaction ${event.transaction.id} to active transaction store for customer ${customer.id}"
    )
  }

  override fun onTransactionError(event: AnchorEvent) {
    log.warn("Received transaction error event: $event")
  }

  override fun onTransactionStatusChanged(event: AnchorEvent) {
    val transaction = event.transaction
    when (val status = transaction.status) {
      SepTransactionStatus.PENDING_ANCHOR -> {
        runBlocking {
          patchTransaction(
            PlatformTransactionData.builder()
              .id(transaction.id)
              .updatedAt(Instant.now())
              .status(SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE)
              .build()
          )
        }
      }
      SepTransactionStatus.COMPLETED -> {
        val customer =
          customerService.getCustomer(
            GetCustomerRequest.builder().account(transaction.destinationAccount).build()
          )
        activeTransactionStore.removeTransaction(customer.id, transaction.id)
        log.info(
          "Removed transaction ${transaction.id} from active transaction store for customer ${customer.id}"
        )
      }
      else -> {
        log.warn("Received transaction status changed event with unsupported status: $status")
      }
    }
  }

  override fun onCustomerUpdated(event: AnchorEvent) {
    val observedAccount = event.customer.id
    val transactionIds = activeTransactionStore.getTransactions(observedAccount)
    log.info(
      "Found ${transactionIds.size} transactions for customer $observedAccount in active transaction store"
    )
    transactionIds.forEach { id ->
      val transaction = runBlocking { platformClient.getTransaction(id) }
      if (transaction.status == SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE) {
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
              .requiredCustomerInfoUpdates(null)
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
