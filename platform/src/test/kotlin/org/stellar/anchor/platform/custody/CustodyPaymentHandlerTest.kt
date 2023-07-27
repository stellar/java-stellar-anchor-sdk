package org.stellar.anchor.platform.custody

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.platform.config.RpcConfig
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo
import org.stellar.anchor.util.FileUtil.getResourceFileAsString
import org.stellar.anchor.util.GsonUtils

class CustodyPaymentHandlerTest {

  // test implementation
  class CustodyPaymentHandlerTestImpl(
    custodyTransactionRepo: JdbcCustodyTransactionRepo,
    platformApiClient: PlatformApiClient,
    rpcConfig: RpcConfig
  ) : CustodyPaymentHandler(custodyTransactionRepo, platformApiClient, rpcConfig) {
    override fun onReceived(txn: JdbcCustodyTransaction?, payment: CustodyPayment?) {
      println("Test implementation")
    }

    override fun onSent(txn: JdbcCustodyTransaction?, payment: CustodyPayment?) {
      println("Test implementation")
    }
  }

  private val gson = GsonUtils.getInstance()

  @MockK(relaxed = true) private lateinit var custodyTransactionRepo: JdbcCustodyTransactionRepo
  @MockK(relaxed = true) private lateinit var platformApiClient: PlatformApiClient
  @MockK(relaxed = true) private lateinit var rpcConfig: RpcConfig

  private lateinit var custodyPaymentHandler: CustodyPaymentHandler

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    custodyPaymentHandler =
      CustodyPaymentHandlerTestImpl(custodyTransactionRepo, platformApiClient, rpcConfig)
  }

  @Test
  fun test_validatePayment_unsupportedType_errorStatus() {
    val txn =
      gson.fromJson(
        getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_transaction_input.json"
        ),
        JdbcCustodyTransaction::class.java
      )
    val payment =
      gson.fromJson(
        getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_payment_unsupported_asset_error_status.json"
        ),
        CustodyPayment::class.java
      )

    custodyPaymentHandler.validatePayment(txn, payment)

    assertNull(payment.getMessage())
  }

  @Test
  fun test_validatePayment_validAsset() {
    val txn =
      gson.fromJson(
        getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_transaction_input.json"
        ),
        JdbcCustodyTransaction::class.java
      )
    val payment =
      gson.fromJson(
        getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_payment_valid_asset.json"
        ),
        CustodyPayment::class.java
      )

    custodyPaymentHandler.validatePayment(txn, payment)

    assertNull(payment.getMessage())
  }

  @Test
  fun test_updateTransaction_withStellarTxId() {
    val txn =
      gson.fromJson(
        getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_transaction_input.json"
        ),
        JdbcCustodyTransaction::class.java
      )
    val payment =
      gson.fromJson(
        getResourceFileAsString("custody/fireblocks/webhook/handler/custody_payment_with_id.json"),
        CustodyPayment::class.java
      )

    val custodyTxCapture = slot<JdbcCustodyTransaction>()
    val txnIdCapture = slot<String>()
    val stellarTxnIdCapture = slot<String>()
    val messageCapture = slot<String>()

    every { rpcConfig.actions.customMessages.outgoingPaymentSent } returns "payment sent"

    every { custodyTransactionRepo.save(capture(custodyTxCapture)) } returns txn
    every {
      platformApiClient.notifyOnchainFundsSent(
        capture(txnIdCapture),
        capture(stellarTxnIdCapture),
        capture(messageCapture)
      )
    } just Runs

    custodyPaymentHandler.updateTransaction(txn, payment, SepTransactionStatus.COMPLETED)

    JSONAssert.assertEquals(
      getResourceFileAsString("custody/fireblocks/webhook/handler/custody_transaction_db.json"),
      gson.toJson(custodyTxCapture.captured),
      JSONCompareMode.STRICT
    )
    assertEquals(txn.id, txnIdCapture.captured)
    assertEquals(payment.transactionHash, stellarTxnIdCapture.captured)
    assertEquals("payment sent", messageCapture.captured)
  }

  @Test
  fun test_updateTransaction_withoutStellarTxId() {
    val txn =
      gson.fromJson(
        getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_transaction_input.json"
        ),
        JdbcCustodyTransaction::class.java
      )
    val payment =
      gson.fromJson(
        getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_payment_without_id.json"
        ),
        CustodyPayment::class.java
      )

    val custodyTxCapture = slot<JdbcCustodyTransaction>()
    val txnIdCapture = slot<String>()
    val stellarTxnIdCapture = slot<String>()
    val messageCapture = slot<String>()

    every { rpcConfig.actions.customMessages.outgoingPaymentSent } returns "payment sent"
    every { custodyTransactionRepo.save(capture(custodyTxCapture)) } returns txn
    every {
      platformApiClient.notifyOnchainFundsSent(
        capture(txnIdCapture),
        capture(stellarTxnIdCapture),
        capture(messageCapture)
      )
    } just Runs

    custodyPaymentHandler.updateTransaction(txn, payment, SepTransactionStatus.COMPLETED)

    JSONAssert.assertEquals(
      getResourceFileAsString("custody/fireblocks/webhook/handler/custody_transaction_db.json"),
      gson.toJson(custodyTxCapture.captured),
      JSONCompareMode.STRICT
    )
    assertEquals(txn.id, txnIdCapture.captured)
    assertEquals(payment.transactionHash, stellarTxnIdCapture.captured)
    assertEquals("payment sent", messageCapture.captured)
  }

  @Test
  fun test_updateTransaction_custody_error() {
    val txn =
      gson.fromJson(
        getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_transaction_input.json"
        ),
        JdbcCustodyTransaction::class.java
      )
    val payment =
      gson.fromJson(
        getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_payment_without_id.json"
        ),
        CustodyPayment::class.java
      )

    val custodyTxCapture = slot<JdbcCustodyTransaction>()
    val txnIdCapture = slot<String>()
    val messageCapture = slot<String>()

    every { rpcConfig.actions.customMessages.custodyTransactionFailed } returns "custody error"
    every { custodyTransactionRepo.save(capture(custodyTxCapture)) } returns txn
    every {
      platformApiClient.notifyTransactionError(capture(txnIdCapture), capture(messageCapture))
    } just Runs

    custodyPaymentHandler.updateTransaction(txn, payment, SepTransactionStatus.ERROR)

    JSONAssert.assertEquals(
      getResourceFileAsString("custody/fireblocks/webhook/handler/custody_transaction_db.json"),
      gson.toJson(custodyTxCapture.captured),
      JSONCompareMode.STRICT
    )
    assertEquals(txn.id, txnIdCapture.captured)
    assertEquals("custody error", messageCapture.captured)
  }
}
