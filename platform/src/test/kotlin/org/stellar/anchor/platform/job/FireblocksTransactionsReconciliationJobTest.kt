package org.stellar.anchor.platform.job

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.stellar.anchor.api.custody.fireblocks.TransactionDetails
import org.stellar.anchor.api.custody.fireblocks.TransactionStatus
import org.stellar.anchor.api.exception.FireblocksException
import org.stellar.anchor.platform.config.FireblocksConfig
import org.stellar.anchor.platform.custody.CustodyPayment
import org.stellar.anchor.platform.custody.CustodyPaymentService
import org.stellar.anchor.platform.custody.CustodyTransactionService
import org.stellar.anchor.platform.custody.fireblocks.FireblocksEventService
import org.stellar.anchor.platform.data.CustodyTransactionStatus
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo

class FireblocksTransactionsReconciliationJobTest {

  companion object {
    private const val EXTERNAL_TXN_ID = "TRANSACTION_ID"
  }

  @MockK(relaxed = true) private lateinit var fireblocksConfig: FireblocksConfig
  @MockK(relaxed = true) private lateinit var custodyPaymentService: CustodyPaymentService
  @MockK(relaxed = true) private lateinit var fireblocksEventService: FireblocksEventService
  @MockK(relaxed = true) private lateinit var custodyTransactionRepo: JdbcCustodyTransactionRepo
  private lateinit var custodyTransactionService: CustodyTransactionService

  private lateinit var reconciliationJob: FireblocksTransactionsReconciliationJob

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    custodyTransactionService =
      CustodyTransactionService(custodyTransactionRepo, custodyPaymentService)

    reconciliationJob =
      FireblocksTransactionsReconciliationJob(
        fireblocksConfig,
        custodyPaymentService,
        fireblocksEventService,
        custodyTransactionService
      )
  }

  @ParameterizedTest
  @EnumSource(
    value = TransactionStatus::class,
    mode = EnumSource.Mode.INCLUDE,
    names = ["COMPLETED", "CANCELLED", "BLOCKED", "FAILED"]
  )
  fun test_reconcileTransactions_successful_reconciliation(status: TransactionStatus) {
    val custodyTransaction =
      JdbcCustodyTransaction.builder()
        .externalTxId(EXTERNAL_TXN_ID)
        .status(CustodyTransactionStatus.SUBMITTED.toString())
        .build()
    val fireblocksTransaction = TransactionDetails.builder().status(status).build()
    val custodyPayment = CustodyPayment.builder().build()

    every {
      custodyTransactionRepo.findAllByStatusAndExternalTxIdNotNull(
        CustodyTransactionStatus.SUBMITTED.toString()
      )
    } returns listOf(custodyTransaction)
    every { custodyPaymentService.getTransactionById(EXTERNAL_TXN_ID) } returns
      fireblocksTransaction
    every { fireblocksEventService.convert(fireblocksTransaction) } returns
      Optional.of(custodyPayment)

    reconciliationJob.reconcileTransactions()

    verify(exactly = 1) { custodyPaymentService.getTransactionById(EXTERNAL_TXN_ID) }
    verify(exactly = 1) { fireblocksEventService.handlePayment(custodyPayment) }
  }

  @Test
  fun test_reconcileTransactions_no_transactions_to_reconcile() {
    every {
      custodyTransactionRepo.findAllByStatusAndExternalTxIdNotNull(
        CustodyTransactionStatus.SUBMITTED.toString()
      )
    } returns emptyList()

    reconciliationJob.reconcileTransactions()

    verify(exactly = 0) { custodyPaymentService.getTransactionById(any()) }
  }

  @ParameterizedTest
  @EnumSource(
    value = TransactionStatus::class,
    mode = EnumSource.Mode.INCLUDE,
    names =
      [
        "QUEUED",
        "PENDING_AUTHORIZATION",
        "PENDING_SIGNATURE",
        "BROADCASTING",
        "PENDING_3RD_PARTY_MANUAL_APPROVAL",
        "PENDING_3RD_PARTY",
        "CONFIRMING",
        "PARTIALLY_COMPLETED",
        "PENDING_AML_SCREENING",
        "REJECTED"
      ]
  )
  fun test_reconcileTransactions_attempt_increase(status: TransactionStatus) {
    val attemptCount = 0
    val custodyTransaction =
      JdbcCustodyTransaction.builder()
        .externalTxId(EXTERNAL_TXN_ID)
        .status(CustodyTransactionStatus.SUBMITTED.toString())
        .reconciliationAttemptCount(attemptCount)
        .build()
    val fireblocksTransaction = TransactionDetails.builder().status(status).build()

    val requestCapture = slot<JdbcCustodyTransaction>()
    every { custodyTransactionService.updateCustodyTransaction(capture(requestCapture)) } just Runs
    every { custodyTransactionService.transactionsEligibleForReconciliation } returns
      listOf(custodyTransaction)
    every { custodyPaymentService.getTransactionById(EXTERNAL_TXN_ID) } returns
      fireblocksTransaction
    every { custodyTransactionRepo.save(any()) } returns null
    every { fireblocksConfig.reconciliation.maxAttempts } returns 10

    reconciliationJob.reconcileTransactions()

    verify(exactly = 1) { custodyPaymentService.getTransactionById(EXTERNAL_TXN_ID) }
    verify(exactly = 1) { custodyTransactionService.updateCustodyTransaction(any()) }

    assertEquals(CustodyTransactionStatus.SUBMITTED.toString(), custodyTransaction.status)
    assertEquals(1, custodyTransaction.reconciliationAttemptCount)
  }

  @Test
  fun test_reconcileTransactions_change_status_to_failed() {
    val attemptCount = 9
    val custodyTransaction =
      JdbcCustodyTransaction.builder()
        .externalTxId(EXTERNAL_TXN_ID)
        .status(CustodyTransactionStatus.SUBMITTED.toString())
        .reconciliationAttemptCount(attemptCount)
        .build()
    val fireblocksTransaction =
      TransactionDetails.builder().status(TransactionStatus.CONFIRMING).build()

    val requestCapture = slot<JdbcCustodyTransaction>()
    every { custodyTransactionService.updateCustodyTransaction(capture(requestCapture)) } just Runs
    every { custodyTransactionService.transactionsEligibleForReconciliation } returns
      listOf(custodyTransaction)
    every { custodyPaymentService.getTransactionById(EXTERNAL_TXN_ID) } returns
      fireblocksTransaction
    every { fireblocksConfig.reconciliation.maxAttempts } returns 10
    every { custodyTransactionRepo.save(any()) } returns null

    reconciliationJob.reconcileTransactions()

    verify(exactly = 1) { custodyPaymentService.getTransactionById(EXTERNAL_TXN_ID) }
    verify(exactly = 1) { custodyTransactionService.updateCustodyTransaction(any()) }

    assertEquals(CustodyTransactionStatus.FAILED.toString(), custodyTransaction.status)
    assertEquals(10, custodyTransaction.reconciliationAttemptCount)
  }

  @Test
  fun test_reconcileTransactions_handle_exception() {
    val attemptCount = 9
    val custodyTransaction =
      JdbcCustodyTransaction.builder()
        .externalTxId(EXTERNAL_TXN_ID)
        .status(CustodyTransactionStatus.SUBMITTED.toString())
        .reconciliationAttemptCount(attemptCount)
        .build()

    every { custodyTransactionService.transactionsEligibleForReconciliation } returns
      listOf(custodyTransaction)
    every { custodyPaymentService.getTransactionById(EXTERNAL_TXN_ID) } throws
      FireblocksException("Too many requests", 429)

    assertDoesNotThrow { reconciliationJob.reconcileTransactions() }
  }
}
