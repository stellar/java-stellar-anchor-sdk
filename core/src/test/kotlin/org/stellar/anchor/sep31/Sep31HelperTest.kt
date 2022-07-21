package org.stellar.anchor.sep31

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

  @Test
  fun test_validateStatus() {
    val txn = PojoSep31Transaction()
    txn.status = "pending_stellar"
    Sep31Helper.validateStatus(txn)
    txn.status = "pending_sender"
    Sep31Helper.validateStatus(txn)
    txn.status = "pending_customer_info_update"
    Sep31Helper.validateStatus(txn)
    txn.status = "pending_transaction_info_update"
    Sep31Helper.validateStatus(txn)
    txn.status = "pending_receiver"
    Sep31Helper.validateStatus(txn)
    txn.status = "pending_sender"
    Sep31Helper.validateStatus(txn)
    txn.status = "completed"
    Sep31Helper.validateStatus(txn)
    txn.status = "error"
    Sep31Helper.validateStatus(txn)

    txn.status = "Error"
    assertThrows<BadRequestException> { Sep31Helper.validateStatus(txn) }
    txn.status = "erroR"
    assertThrows<BadRequestException> { Sep31Helper.validateStatus(txn) }
    txn.status = ""
    assertThrows<BadRequestException> { Sep31Helper.validateStatus(txn) }
    txn.status = null
    assertThrows<BadRequestException> { Sep31Helper.validateStatus(txn) }
  }
}
