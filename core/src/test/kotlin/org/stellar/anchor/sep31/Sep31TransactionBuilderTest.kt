package org.stellar.anchor.sep31

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Sep31TransactionBuilderTest {
  @MockK(relaxed = true) private lateinit var txnStore: Sep31TransactionStore

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun test_RefundBuilder() {
    every { txnStore.newRefunds() } returns PojoSep31Refunds()
    every { txnStore.newRefundPayment() } returns PojoSep31RefundPayment()

    val refundPayments =
        listOf(
            RefundPaymentBuilder(txnStore)
                .id("de762cda-a193-4961-861e-57b31fed6eb3")
                .amount("10")
                .fee("1")
                .build(),
            (RefundPaymentBuilder(txnStore)
                .id("aa762cda-a193-4961-861e-57b31fed6eb3")
                .amount("20")
                .fee("1")
                .build()))

    val refunds =
        RefundsBuilder(txnStore)
            .amountRefunded("32")
            .amountFee("3")
            .payments(refundPayments)
            .build()

    assertEquals("32", refunds.amountRefunded)
    assertEquals("3", refunds.amountFee)
    assertEquals(2, refunds.refundPayments.size)
  }
}
