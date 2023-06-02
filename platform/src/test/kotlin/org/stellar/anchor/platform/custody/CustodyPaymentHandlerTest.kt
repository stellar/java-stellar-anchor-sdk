package org.stellar.anchor.platform.custody

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.platform.PatchTransactionsRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo
import org.stellar.anchor.util.FileUtil.getResourceFileAsString
import org.stellar.anchor.util.GsonUtils

class CustodyPaymentHandlerTest {

  // test implementation
  class CustodyPaymentHandlerTestImpl(
    custodyTransactionRepo: JdbcCustodyTransactionRepo,
    platformApiClient: PlatformApiClient
  ) : CustodyPaymentHandler(custodyTransactionRepo, platformApiClient) {
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

  private lateinit var custodyPaymentHandler: CustodyPaymentHandler

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    custodyPaymentHandler = CustodyPaymentHandlerTestImpl(custodyTransactionRepo, platformApiClient)
  }

  @Test
  fun test_validatePayment_unsupportedType_successStatus() {
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
          "custody/fireblocks/webhook/handler/custody_payment_unsupported_asset_success_status.json"
        ),
        CustodyPayment::class.java
      )

    custodyPaymentHandler.validatePayment(txn, payment)

    assertEquals("Unsupported asset type", payment.getMessage())
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
  fun test_validatePayment_differentAssets() {
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
          "custody/fireblocks/webhook/handler/custody_payment_different_asset.json"
        ),
        CustodyPayment::class.java
      )

    custodyPaymentHandler.validatePayment(txn, payment)

    assertEquals("Incoming asset does not match the expected asset", payment.getMessage())
  }

  @Test
  fun test_validatePayment_invalidAmount() {
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
          "custody/fireblocks/webhook/handler/custody_payment_invalid_amount.json"
        ),
        CustodyPayment::class.java
      )

    custodyPaymentHandler.validatePayment(txn, payment)

    assertEquals("The incoming payment amount was insufficient", payment.getMessage())
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
    val patchTxRequestCapture = slot<PatchTransactionsRequest>()

    every { custodyTransactionRepo.save(capture(custodyTxCapture)) } returns txn
    every { platformApiClient.patchTransaction(capture(patchTxRequestCapture)) } returns null

    custodyPaymentHandler.updateTransaction(txn, payment, SepTransactionStatus.COMPLETED)

    JSONAssert.assertEquals(
      getResourceFileAsString("custody/fireblocks/webhook/handler/custody_transaction_db.json"),
      gson.toJson(custodyTxCapture.captured),
      JSONCompareMode.STRICT
    )
    JSONAssert.assertEquals(
      getResourceFileAsString(
        "custody/fireblocks/webhook/handler/patch_transaction_request_with_id.json"
      ),
      gson.toJson(patchTxRequestCapture.captured),
      JSONCompareMode.STRICT
    )
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
    val patchTxRequestCapture = slot<PatchTransactionsRequest>()

    every { custodyTransactionRepo.save(capture(custodyTxCapture)) } returns txn
    every { platformApiClient.patchTransaction(capture(patchTxRequestCapture)) } returns null

    custodyPaymentHandler.updateTransaction(txn, payment, SepTransactionStatus.COMPLETED)

    JSONAssert.assertEquals(
      getResourceFileAsString("custody/fireblocks/webhook/handler/custody_transaction_db.json"),
      gson.toJson(custodyTxCapture.captured),
      JSONCompareMode.STRICT
    )
    JSONAssert.assertEquals(
      getResourceFileAsString(
        "custody/fireblocks/webhook/handler/patch_transaction_request_without_id.json"
      ),
      gson.toJson(patchTxRequestCapture.captured),
      JSONCompareMode.STRICT
    )
  }
}
