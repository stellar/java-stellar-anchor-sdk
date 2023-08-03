package org.stellar.anchor.platform.custody

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo
import org.stellar.anchor.util.FileUtil.getResourceFileAsString
import org.stellar.anchor.util.GsonUtils

class CustodyPaymentHandlerTest {

  // test implementation
  class CustodyPaymentHandlerTestImpl(custodyTransactionRepo: JdbcCustodyTransactionRepo) :
    CustodyPaymentHandler(custodyTransactionRepo) {
    override fun onReceived(txn: JdbcCustodyTransaction?, payment: CustodyPayment?) {
      println("Test implementation")
    }

    override fun onSent(txn: JdbcCustodyTransaction?, payment: CustodyPayment?) {
      println("Test implementation")
    }
  }

  private val gson = GsonUtils.getInstance()

  @MockK(relaxed = true) private lateinit var custodyTransactionRepo: JdbcCustodyTransactionRepo

  private lateinit var custodyPaymentHandler: CustodyPaymentHandler

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    custodyPaymentHandler = CustodyPaymentHandlerTestImpl(custodyTransactionRepo)
  }

  @Test
  fun test_validatePayment_unsupportedType_errorStatus() {
    val txn =
      gson.fromJson(
        getResourceFileAsString(
          "custody/fireblocks/webhook/handler/custody_transaction_input_sep24_deposit_payment.json"
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
          "custody/fireblocks/webhook/handler/custody_transaction_input_sep24_deposit_payment.json"
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
          "custody/fireblocks/webhook/handler/custody_transaction_input_sep24_deposit_payment.json"
        ),
        JdbcCustodyTransaction::class.java
      )
    val payment =
      gson.fromJson(
        getResourceFileAsString("custody/fireblocks/webhook/handler/custody_payment_with_id.json"),
        CustodyPayment::class.java
      )

    val custodyTxCapture = slot<JdbcCustodyTransaction>()

    every { custodyTransactionRepo.save(capture(custodyTxCapture)) } returns txn

    custodyPaymentHandler.updateTransaction(txn, payment)

    JSONAssert.assertEquals(
      getResourceFileAsString(
        "custody/fireblocks/webhook/handler/custody_transaction_db_sep24_deposit_payment.json"
      ),
      gson.toJson(custodyTxCapture.captured),
      JSONCompareMode.STRICT
    )
  }
}
