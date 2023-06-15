package org.stellar.anchor.platform.custody

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.platform.PatchTransactionsRequest
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo
import org.stellar.anchor.util.FileUtil
import org.stellar.anchor.util.GsonUtils

class Sep31CustodyPaymentHandlerTest {

  @MockK(relaxed = true) private lateinit var custodyTransactionRepo: JdbcCustodyTransactionRepo

  @MockK(relaxed = true) private lateinit var platformApiClient: PlatformApiClient

  @MockK(relaxed = true) private lateinit var sep31TransactionCounter: Counter

  @MockK(relaxed = true) private lateinit var paymentReceivedCounter: Counter

  private lateinit var sep31CustodyPaymentHandler: Sep31CustodyPaymentHandler

  private val gson = GsonUtils.getInstance()

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    sep31CustodyPaymentHandler =
      Sep31CustodyPaymentHandler(custodyTransactionRepo, platformApiClient)
  }

  @Test
  fun test_handleEvent_onReceived_successStatus() {
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
    val patchTxRequestCapture = slot<PatchTransactionsRequest>()

    mockkStatic(Metrics::class)

    every { custodyTransactionRepo.save(capture(custodyTxCapture)) } returns txn
    every { platformApiClient.patchTransaction(capture(patchTxRequestCapture)) } returns null
    every { Metrics.counter("sep31.transaction", "status", "pending_receiver") } returns
      sep31TransactionCounter
    every { Metrics.counter("payment.received", "asset", "testAmountInAsset") } returns
      paymentReceivedCounter

    sep31CustodyPaymentHandler.onReceived(txn, payment)

    verify(exactly = 1) { sep31TransactionCounter.increment() }
    verify(exactly = 1) { paymentReceivedCounter.increment(1.0000000) }

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString(
        "custody/fireblocks/webhook/handler/custody_transaction_db.json"
      ),
      gson.toJson(custodyTxCapture.captured),
      JSONCompareMode.STRICT
    )
    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString(
        "custody/fireblocks/webhook/handler/patch_transaction_request_with_id_sep31_receive.json"
      ),
      gson.toJson(patchTxRequestCapture.captured),
      JSONCompareMode.STRICT
    )
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

    mockkStatic(Metrics::class)

    sep31CustodyPaymentHandler.onSent(txn, payment)

    verify(exactly = 0) { Metrics.counter("sep31.transaction", any(), any()) }
    verify(exactly = 0) { Metrics.counter("payment.sent", any(), any()) }
    verify(exactly = 0) { custodyTransactionRepo.save(any()) }
    verify(exactly = 0) { platformApiClient.patchTransaction(any()) }
  }
}
