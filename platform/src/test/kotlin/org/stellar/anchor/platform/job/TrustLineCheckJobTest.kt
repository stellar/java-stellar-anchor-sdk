package org.stellar.anchor.platform.job

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR
import org.stellar.anchor.custody.CustodyService
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.platform.config.PropertyCustodyConfig
import org.stellar.anchor.platform.config.PropertyCustodyConfig.TrustLine
import org.stellar.anchor.platform.data.*
import org.stellar.anchor.platform.service.TransactionService
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.util.GsonUtils

class TrustLineCheckJobTest {

  companion object {
    private val gson = GsonUtils.getInstance()
    private const val TX_ID = "testId"
    private const val ACCOUNT = "testToAccount"
    private const val ASSET = "testAmountOutAsset"
    private const val TX_MESSAGE = "testMessage"
  }

  @MockK(relaxed = true) private lateinit var horizon: Horizon
  @MockK(relaxed = true)
  private lateinit var transactionPendingTrustRepo: JdbcTransactionPendingTrustRepo
  @MockK(relaxed = true) private lateinit var custodyConfig: PropertyCustodyConfig
  @MockK(relaxed = true) private lateinit var transactionService: TransactionService
  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore
  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore
  @MockK(relaxed = true) private lateinit var custodyService: CustodyService

  private lateinit var trustLineCheckJob: TrustLineCheckJob

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    trustLineCheckJob =
      TrustLineCheckJob(
        horizon,
        transactionPendingTrustRepo,
        custodyConfig,
        transactionService,
        txn24Store,
        txn31Store,
        custodyService
      )
  }

  @Test
  fun test_checkNotTimedOut_trustNotConfigured() {
    val createdAt = Instant.now()
    val txnPendingTrust = JdbcTransactionPendingTrust()
    txnPendingTrust.id = TX_ID
    txnPendingTrust.createdAt = createdAt
    txnPendingTrust.count = 0
    txnPendingTrust.asset = ASSET
    txnPendingTrust.account = ACCOUNT
    val trustLine = TrustLine()
    trustLine.checkDuration = 10
    val txnPendingTrustCapture = slot<JdbcTransactionPendingTrust>()

    every { transactionPendingTrustRepo.findAll() } returns listOf(txnPendingTrust)
    every { custodyConfig.trustLine } returns trustLine
    every { transactionPendingTrustRepo.save(capture(txnPendingTrustCapture)) } returns null
    every { horizon.isTrustLineConfigured(ACCOUNT, ASSET) } returns false

    trustLineCheckJob.checkTrust()

    verify { custodyService wasNot Called }
    verify { txn24Store wasNot Called }
    verify { txn31Store wasNot Called }
    verify(exactly = 0) { transactionPendingTrustRepo.delete(any()) }

    val expectedTxnPendingTrust = JdbcTransactionPendingTrust()
    expectedTxnPendingTrust.id = TX_ID
    expectedTxnPendingTrust.count = 1
    expectedTxnPendingTrust.createdAt = createdAt
    expectedTxnPendingTrust.account = ACCOUNT
    expectedTxnPendingTrust.asset = ASSET

    JSONAssert.assertEquals(
      gson.toJson(expectedTxnPendingTrust),
      gson.toJson(txnPendingTrustCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_checkNotTimedOut_trustConfigured() {
    val createdAt = Instant.now()
    val txnPendingTrust = JdbcTransactionPendingTrust()
    txnPendingTrust.id = TX_ID
    txnPendingTrust.createdAt = createdAt
    txnPendingTrust.count = 0
    txnPendingTrust.asset = ASSET
    txnPendingTrust.account = ACCOUNT
    val trustLine = TrustLine()
    trustLine.checkDuration = 10

    every { transactionPendingTrustRepo.findAll() } returns listOf(txnPendingTrust)
    every { custodyConfig.trustLine } returns trustLine
    every { horizon.isTrustLineConfigured(ACCOUNT, ASSET) } returns true

    trustLineCheckJob.checkTrust()

    verify(exactly = 1) { custodyService.createTransactionPayment(TX_ID, null) }
    verify(exactly = 1) { transactionPendingTrustRepo.delete(txnPendingTrust) }
    verify(exactly = 0) { transactionPendingTrustRepo.save(any()) }
    verify { txn24Store wasNot Called }
    verify { txn31Store wasNot Called }
  }

  @Test
  fun test_checkTimedOut_sep24() {
    val createdAt = Instant.now().minus(11, ChronoUnit.HOURS)
    val txnPendingTrust = JdbcTransactionPendingTrust()
    txnPendingTrust.id = TX_ID
    txnPendingTrust.createdAt = createdAt
    txnPendingTrust.count = 0
    txnPendingTrust.asset = ASSET
    txnPendingTrust.account = ACCOUNT
    val trustLine = TrustLine()
    trustLine.checkDuration = 10
    trustLine.timeoutMessage = TX_MESSAGE
    val txn24 = JdbcSep24Transaction()
    txn24.id = TX_ID
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { transactionPendingTrustRepo.findAll() } returns listOf(txnPendingTrust)
    every { custodyConfig.trustLine } returns trustLine
    every { transactionService.findTransaction(TX_ID) } returns txn24
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val startDate = Instant.now()
    trustLineCheckJob.checkTrust()
    val endDate = Instant.now()

    verify(exactly = 1) { transactionPendingTrustRepo.delete(txnPendingTrust) }
    verify(exactly = 0) { transactionPendingTrustRepo.save(any()) }
    verify { custodyService wasNot Called }
    verify { horizon wasNot Called }
    verify { txn31Store wasNot Called }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.id = TX_ID
    expectedSep24Txn.message = TX_MESSAGE
    expectedSep24Txn.status = PENDING_ANCHOR.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedSep24Txn.updatedAt >= startDate)
    assertTrue(expectedSep24Txn.updatedAt <= endDate)
  }

  @Test
  fun test_checkTimedOut_sep31() {
    val createdAt = Instant.now().minus(11, ChronoUnit.HOURS)
    val txnPendingTrust = JdbcTransactionPendingTrust()
    txnPendingTrust.id = TX_ID
    txnPendingTrust.createdAt = createdAt
    txnPendingTrust.count = 0
    txnPendingTrust.asset = ASSET
    txnPendingTrust.account = ACCOUNT
    val trustLine = TrustLine()
    trustLine.checkDuration = 10
    trustLine.timeoutMessage = TX_MESSAGE
    val txn31 = JdbcSep31Transaction()
    txn31.id = TX_ID
    val sep31TxnCapture = slot<JdbcSep31Transaction>()

    every { transactionPendingTrustRepo.findAll() } returns listOf(txnPendingTrust)
    every { custodyConfig.trustLine } returns trustLine
    every { transactionService.findTransaction(TX_ID) } returns txn31
    every { txn31Store.save(capture(sep31TxnCapture)) } returns null

    val startDate = Instant.now()
    trustLineCheckJob.checkTrust()
    val endDate = Instant.now()

    verify(exactly = 1) { transactionPendingTrustRepo.delete(txnPendingTrust) }
    verify(exactly = 0) { transactionPendingTrustRepo.save(any()) }
    verify { custodyService wasNot Called }
    verify { horizon wasNot Called }
    verify { txn24Store wasNot Called }

    val expectedSep31Txn = JdbcSep31Transaction()
    expectedSep31Txn.id = TX_ID
    expectedSep31Txn.requiredInfoMessage = TX_MESSAGE
    expectedSep31Txn.status = PENDING_ANCHOR.toString()
    expectedSep31Txn.updatedAt = sep31TxnCapture.captured.updatedAt

    JSONAssert.assertEquals(
      gson.toJson(expectedSep31Txn),
      gson.toJson(sep31TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedSep31Txn.updatedAt >= startDate)
    assertTrue(expectedSep31Txn.updatedAt <= endDate)
  }
}
