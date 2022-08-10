package org.stellar.anchor.sep31

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.RefundPayment

class RefundsTest {
  companion object {
    private const val fiatUSD = "iso4217:USD"
    private const val stellarUSDC =
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
  }

  @MockK(relaxed = true) private lateinit var sep31TransactionStore: Sep31TransactionStore

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { sep31TransactionStore.newRefunds() } answers { PojoSep31Refunds() }
    every { sep31TransactionStore.newRefundPayment() } answers { PojoSep31RefundPayment() }
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_loadPlatformApiRefunds() {
    // Mock the SEP-31 Refunds object we want
    val mockRefundPayment1 = PojoSep31RefundPayment()
    mockRefundPayment1.id = "A"
    mockRefundPayment1.amount = "50"
    mockRefundPayment1.fee = "4"
    val mockRefundPayment2 = PojoSep31RefundPayment()
    mockRefundPayment2.id = "B"
    mockRefundPayment2.amount = "50"
    mockRefundPayment2.fee = "4"
    val mockRefundPaymentList = listOf(mockRefundPayment1, mockRefundPayment2)
    val wantSep31Refunds = PojoSep31Refunds()
    wantSep31Refunds.amountRefunded = "100"
    wantSep31Refunds.amountFee = "8"
    wantSep31Refunds.refundPayments = mockRefundPaymentList

    // mock the PlatformApi Refund
    val platformApiRefund =
      org.stellar.anchor.api.shared.Refund.builder()
        .amountRefunded(Amount("100", fiatUSD))
        .amountFee(Amount("8", stellarUSDC))
        .payments(
          arrayOf(
            RefundPayment.builder()
              .id("A")
              .idType(RefundPayment.IdType.STELLAR)
              .amount(Amount("50", stellarUSDC))
              .fee(Amount("4", stellarUSDC))
              .refundedAt(Instant.now())
              .refundedAt(Instant.now())
              .build(),
            RefundPayment.builder()
              .id("B")
              .idType(RefundPayment.IdType.STELLAR)
              .amount(Amount("50", stellarUSDC))
              .fee(Amount("4", stellarUSDC))
              .refundedAt(Instant.now())
              .refundedAt(Instant.now())
              .build()
          )
        )
        .build()

    // build the SEP-31 Refunds object
    val gotSep31Refunds = Refunds.of(platformApiRefund, sep31TransactionStore)
    assertEquals(wantSep31Refunds, gotSep31Refunds)
  }
}
