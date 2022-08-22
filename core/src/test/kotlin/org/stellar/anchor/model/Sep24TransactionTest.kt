package org.stellar.anchor.model

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.stellar.anchor.sep24.PojoSep24Transaction
import org.stellar.anchor.sep24.Sep24TransactionBuilder
import org.stellar.anchor.sep24.Sep24TransactionStore

internal class Sep24TransactionTest {
  @Test
  fun `test the Sep24TransactionBuilder builds the Sep24Transaction correctly`() {
    val store = mockk<Sep24TransactionStore>()
    every { store.newInstance() } returns PojoSep24Transaction()
    val builder = Sep24TransactionBuilder(store)
    var txn =
      builder
        .transactionId("txnId")
        .completedAt(10)
        .withdrawAnchorAccount("account")
        .memo("memo")
        .amountFee("20")
        .amountInAsset("USDC_In")
        .amountOutAsset("USDC_Out")
        .amountFeeAsset("USDC_Fee")
        .build()

    assertEquals(txn.transactionId, "txnId")
    assertEquals(txn.completedAt, 10)
    assertEquals(txn.withdrawAnchorAccount, "account")
    assertEquals(txn.memo, "memo")
    assertEquals(txn.amountFee, "20")
    assertEquals(txn.amountInAsset, "USDC_In")
    assertEquals(txn.amountOutAsset, "USDC_Out")
    assertEquals(txn.amountFeeAsset, "USDC_Fee")
  }
}
