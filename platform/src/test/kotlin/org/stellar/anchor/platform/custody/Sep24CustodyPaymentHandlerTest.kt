package org.stellar.anchor.platform.custody

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.platform.PlatformTransactionData
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.platform.config.RpcConfig
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo
import org.stellar.anchor.util.FileUtil
import org.stellar.anchor.util.GsonUtils

class Sep24CustodyPaymentHandlerTest {

  @MockK(relaxed = true) private lateinit var custodyTransactionRepo: JdbcCustodyTransactionRepo
  @MockK(relaxed = true) private lateinit var platformApiClient: PlatformApiClient
  @MockK(relaxed = true) private lateinit var sep24TransactionCounter: Counter
  @MockK(relaxed = true) private lateinit var paymentReceivedCounter: Counter
  @MockK(relaxed = true) private lateinit var paymentSentCounter: Counter
  @MockK(relaxed = true) private lateinit var rpcConfig: RpcConfig

  private lateinit var sep24CustodyPaymentHandler: Sep24CustodyPaymentHandler

  private val gson = GsonUtils.getInstance()

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    sep24CustodyPaymentHandler =
      Sep24CustodyPaymentHandler(custodyTransactionRepo, platformApiClient, rpcConfig)
  }

  @Test
  fun test_handleEvent_onReceived() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_transaction_input.json"
        ),
        JdbcCustodyTransaction::class.java
      )
    txn.kind = PlatformTransactionData.Kind.WITHDRAWAL.kind
    val payment =
      gson.fromJson(
        FileUtil.getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_payment_with_id.json"
        ),
        CustodyPayment::class.java
      )

    val custodyTxCapture = slot<JdbcCustodyTransaction>()
    val txnIdCapture = slot<String>()
    val stellarTxnIdCapture = slot<String>()
    val amountCapture = slot<String>()
    val messageCapture = slot<String>()

    mockkStatic(Metrics::class)

    every { rpcConfig.customMessages.incomingPaymentReceived } returns "payment received"
    every { custodyTransactionRepo.save(capture(custodyTxCapture)) } returns txn
    every {
      platformApiClient.notifyOnchainFundsReceived(
        capture(txnIdCapture),
        capture(stellarTxnIdCapture),
        capture(amountCapture),
        capture(messageCapture)
      )
    } just Runs
    every { Metrics.counter("sep24.transaction", "status", "pending_anchor") } returns
      sep24TransactionCounter
    every { Metrics.counter("payment.received", "asset", "testAmountInAsset") } returns
      paymentReceivedCounter

    sep24CustodyPaymentHandler.onReceived(txn, payment)

    verify(exactly = 1) { sep24TransactionCounter.increment() }
    verify(exactly = 1) { paymentReceivedCounter.increment(1.0000000) }

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString(
        "custody/fireblocks/webhook/handler/custody_transaction_db_withdrawal.json"
      ),
      gson.toJson(custodyTxCapture.captured),
      JSONCompareMode.STRICT
    )
    Assertions.assertEquals(txn.id, txnIdCapture.captured)
    Assertions.assertEquals(payment.transactionHash, stellarTxnIdCapture.captured)
    Assertions.assertEquals(payment.amount, amountCapture.captured)
    Assertions.assertEquals("payment received", messageCapture.captured)
  }

  @Test
  fun test_handleEvent_onSent() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_transaction_input.json"
        ),
        JdbcCustodyTransaction::class.java
      )
    val payment =
      gson.fromJson(
        FileUtil.getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_payment_with_id.json"
        ),
        CustodyPayment::class.java
      )

    val custodyTxCapture = slot<JdbcCustodyTransaction>()
    val txnIdCapture = slot<String>()
    val stellarTxnIdCapture = slot<String>()
    val messageCapture = slot<String>()

    mockkStatic(Metrics::class)

    every { rpcConfig.customMessages.outgoingPaymentSent } returns "payment sent"
    every { custodyTransactionRepo.save(capture(custodyTxCapture)) } returns txn
    every {
      platformApiClient.notifyOnchainFundsSent(
        capture(txnIdCapture),
        capture(stellarTxnIdCapture),
        capture(messageCapture)
      )
    } just Runs
    every { Metrics.counter("sep24.transaction", "status", "completed") } returns
      sep24TransactionCounter
    every { Metrics.counter("payment.sent", "asset", "testAmountInAsset") } returns
      paymentSentCounter

    sep24CustodyPaymentHandler.onSent(txn, payment)

    verify(exactly = 1) { sep24TransactionCounter.increment() }
    verify(exactly = 1) { paymentSentCounter.increment(1.0000000) }

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString(
        "custody/fireblocks/webhook/handler/custody_transaction_db.json"
      ),
      gson.toJson(custodyTxCapture.captured),
      JSONCompareMode.STRICT
    )
    Assertions.assertEquals(txn.id, txnIdCapture.captured)
    Assertions.assertEquals(payment.transactionHash, stellarTxnIdCapture.captured)
    Assertions.assertEquals("payment sent", messageCapture.captured)
  }
}
