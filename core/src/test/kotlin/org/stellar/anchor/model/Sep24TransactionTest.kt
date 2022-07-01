package org.stellar.anchor.model

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.sep24.PojoSep24Transaction
import org.stellar.anchor.sep24.Sep24TransactionBuilder
import org.stellar.anchor.sep24.Sep24TransactionStore

internal class Sep24TransactionTest {
  @Test
  fun testStatusCoverage() {
    SepTransactionStatus.COMPLETED.description
  }

  @Test
  fun testBuilder() {
    val store = mockk<Sep24TransactionStore>()
    every { store.newInstance() } returns PojoSep24Transaction()
    val builder = Sep24TransactionBuilder(store)
    builder
      .transactionId("txnId")
      .completedAt(10)
      .withdrawAnchorAccount("account")
      .memo("memo")
      .amountFee("20")
      .amountInAsset("USDC_In")
      .amountOutAsset("USDC_Out")
      .amountFeeAsset("USDC_Fee")
  }
}
