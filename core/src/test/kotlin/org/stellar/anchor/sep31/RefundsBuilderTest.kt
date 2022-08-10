package org.stellar.anchor.sep31

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RefundsBuilderTest {
  @MockK(relaxed = true) private lateinit var sep31TransactionStore: Sep31TransactionStore

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { sep31TransactionStore.newRefunds() } returns PojoSep31Refunds()
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_allBuilderFields() {
    // mock the refundPaymentsList
    val refundPayment1 = PojoSep31RefundPayment()
    refundPayment1.id = "111"
    refundPayment1.amount = "5"
    refundPayment1.fee = "1"
    val refundPayment2 = PojoSep31RefundPayment()
    refundPayment2.id = "222"
    refundPayment2.amount = "5"
    refundPayment2.fee = "1"
    val refundPaymentList = listOf(refundPayment1, refundPayment2)

    // mock the Refunds object we want
    val wantRefunds = PojoSep31Refunds()
    wantRefunds.amountRefunded = "10"
    wantRefunds.amountFee = "2"
    wantRefunds.refundPayments = refundPaymentList

    // build the Refunds object.
    val gotRefunds =
      RefundsBuilder(sep31TransactionStore)
        .amountRefunded("10")
        .amountFee("2")
        .payments(refundPaymentList)
        .build()
    assertEquals(wantRefunds, gotRefunds)
  }
}
