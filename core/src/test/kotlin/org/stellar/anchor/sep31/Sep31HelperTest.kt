package org.stellar.anchor.sep31

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.api.exception.BadRequestException

class Sep31HelperTest {
  @Test
  fun test_allAmountAvailable() {
    val txn = PojoSep31Transaction()
    txn.amountIn = "100"
    assertFalse(Sep31Helper.allAmountAvailable(txn))
    txn.amountInAsset = "USDC"
    assertFalse(Sep31Helper.allAmountAvailable(txn))
    txn.amountOut = "500"
    assertFalse(Sep31Helper.allAmountAvailable(txn))
    txn.amountOutAsset = "BRL"
    assertFalse(Sep31Helper.allAmountAvailable(txn))
    txn.amountFee = "5"
    assertFalse(Sep31Helper.allAmountAvailable(txn))
    txn.amountFeeAsset = "USDC"
    assertTrue(Sep31Helper.allAmountAvailable(txn))
  }

  @ParameterizedTest
  @ValueSource(
    strings =
      [
        "pending_stellar",
        "pending_sender",
        "pending_customer_info_update",
        "pending_transaction_info_update",
        "pending_receiver",
        "pending_sender",
        "completed",
        "error"]
  )
  fun test_validateStatus(status: String) {
    val txn = PojoSep31Transaction()
    txn.status = status
    Sep31Helper.validateStatus(txn)
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["Error", "erroR", ""])
  fun test_validateStatus_failure(status: String?) {
    val txn = PojoSep31Transaction()
    txn.status = status
    val ex = assertThrows<BadRequestException> { Sep31Helper.validateStatus(txn) }
    assertEquals("'$status' is not a valid status of SEP31.", ex.message)
  }
}
