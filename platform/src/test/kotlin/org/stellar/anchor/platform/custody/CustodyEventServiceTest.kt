package org.stellar.anchor.platform.custody

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo

class CustodyEventServiceTest {

  // test implementation
  class CustodyEventServiceTestImpl(
    custodyTransactionRepo: JdbcCustodyTransactionRepo,
    sep24CustodyPaymentHandler: Sep24CustodyPaymentHandler,
    sep31CustodyPaymentHandler: Sep31CustodyPaymentHandler
  ) :
    CustodyEventService(
      custodyTransactionRepo,
      sep24CustodyPaymentHandler,
      sep31CustodyPaymentHandler
    ) {
    override fun handleEvent(event: String?, headers: MutableMap<String, String>?) {
      println("Test implementation")
    }
  }

  @MockK(relaxed = true) private lateinit var custodyTransactionRepo: JdbcCustodyTransactionRepo
  @MockK(relaxed = true) private lateinit var sep24CustodyPaymentHandler: Sep24CustodyPaymentHandler
  @MockK(relaxed = true) private lateinit var sep31CustodyPaymentHandler: Sep31CustodyPaymentHandler

  private lateinit var custodyEventService: CustodyEventService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    custodyEventService =
      CustodyEventServiceTestImpl(
        custodyTransactionRepo,
        sep24CustodyPaymentHandler,
        sep31CustodyPaymentHandler
      )
  }

  @Test
  fun test_handleEvent_transactionIsNotFound() {
    val payment =
      CustodyPayment.builder()
        .externalTxId("testExternalTxId")
        .to("testTo")
        .transactionMemo("testMemo")
        .assetType("credit_alphanum4")
        .build()

    every { custodyTransactionRepo.findByExternalTxId(any()) } returns null
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    custodyEventService.handlePayment(payment)

    verify(exactly = 0) { sep24CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep24CustodyPaymentHandler.onSent(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onSent(any(), any()) }
  }

  @Test
  fun test_handleEvent_notSupportedAssetType() {
    val payment =
      CustodyPayment.builder()
        .externalTxId("testExternalTxId")
        .to("testTo")
        .transactionMemo("testMemo")
        .assetType("test_credit_alphanum4")
        .build()

    every { custodyTransactionRepo.findByExternalTxId(any()) } returns null
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    custodyEventService.handlePayment(payment)

    verify(exactly = 0) { sep24CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep24CustodyPaymentHandler.onSent(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onSent(any(), any()) }
  }

  @Test
  fun test_handleEvent_sep24_receive() {
    val payment =
      CustodyPayment.builder()
        .externalTxId("testExternalTxId")
        .to("testTo")
        .transactionMemo("testMemo")
        .assetType("credit_alphanum4")
        .build()
    val txn = JdbcCustodyTransaction.builder().kind("receive").protocol("24").build()

    every { custodyTransactionRepo.findByExternalTxId("testExternalTxId") } returns txn
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    custodyEventService.handlePayment(payment)

    verify(exactly = 0) { sep24CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep24CustodyPaymentHandler.onSent(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onSent(any(), any()) }
  }

  @Test
  fun test_handleEvent_sep24_deposit() {
    val payment =
      CustodyPayment.builder()
        .externalTxId("testExternalTxId")
        .to("testTo")
        .transactionMemo("testMemo")
        .assetType("credit_alphanum4")
        .build()
    val txn = JdbcCustodyTransaction.builder().kind("deposit").protocol("24").build()

    every { custodyTransactionRepo.findByExternalTxId("testExternalTxId") } returns txn
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    custodyEventService.handlePayment(payment)

    verify(exactly = 0) { sep24CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 1) { sep24CustodyPaymentHandler.onSent(txn, payment) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onSent(any(), any()) }
  }

  @Test
  fun test_handleEvent_sep24_withdrawal() {
    val payment =
      CustodyPayment.builder()
        .externalTxId("testExternalTxId")
        .to("testTo")
        .transactionMemo("testMemo")
        .assetType("credit_alphanum4")
        .build()
    val txn = JdbcCustodyTransaction.builder().kind("withdrawal").protocol("24").build()

    every { custodyTransactionRepo.findByExternalTxId(any()) } returns null
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc("testTo", "testMemo")
    } returns txn

    custodyEventService.handlePayment(payment)

    verify(exactly = 1) { sep24CustodyPaymentHandler.onReceived(txn, payment) }
    verify(exactly = 0) { sep24CustodyPaymentHandler.onSent(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onSent(any(), any()) }
  }

  @Test
  fun test_handleEvent_sep31_deposit() {
    val payment =
      CustodyPayment.builder()
        .externalTxId("testExternalTxId")
        .to("testTo")
        .transactionMemo("testMemo")
        .assetType("credit_alphanum4")
        .build()
    val txn = JdbcCustodyTransaction.builder().kind("deposit").protocol("31").build()

    every { custodyTransactionRepo.findByExternalTxId("testExternalTxId") } returns txn
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc(any(), any())
    } returns null

    custodyEventService.handlePayment(payment)

    verify(exactly = 0) { sep24CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep24CustodyPaymentHandler.onSent(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onSent(any(), any()) }
  }

  @Test
  fun test_handleEvent_sep31_receive() {
    val payment =
      CustodyPayment.builder()
        .externalTxId("testExternalTxId")
        .to("testTo")
        .transactionMemo("testMemo")
        .assetType("credit_alphanum4")
        .build()
    val txn = JdbcCustodyTransaction.builder().kind("receive").protocol("31").build()

    every { custodyTransactionRepo.findByExternalTxId(any()) } returns txn
    every {
      custodyTransactionRepo.findFirstByToAccountAndMemoOrderByCreatedAtDesc("testTo", "testMemo")
    } returns null

    custodyEventService.handlePayment(payment)

    verify(exactly = 0) { sep24CustodyPaymentHandler.onReceived(any(), any()) }
    verify(exactly = 0) { sep24CustodyPaymentHandler.onSent(any(), any()) }
    verify(exactly = 1) { sep31CustodyPaymentHandler.onReceived(txn, payment) }
    verify(exactly = 0) { sep31CustodyPaymentHandler.onSent(any(), any()) }
  }
}
