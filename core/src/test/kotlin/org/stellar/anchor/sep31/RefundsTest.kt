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
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse
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
  fun `test conversion to sep31 transaction response refunds`() {
    // mock the SEP-31 Refunds object
    val mockRefundPayment1 = PojoSep31RefundPayment()
    mockRefundPayment1.id = "A"
    mockRefundPayment1.amount = "50"
    mockRefundPayment1.fee = "4"
    val mockRefundPayment2 = PojoSep31RefundPayment()
    mockRefundPayment2.id = "B"
    mockRefundPayment2.amount = "50"
    mockRefundPayment2.fee = "4"
    val mockRefundPaymentList = listOf(mockRefundPayment1, mockRefundPayment2)
    val mockSep31Refunds = PojoSep31Refunds()
    mockSep31Refunds.amountRefunded = "100"
    mockSep31Refunds.amountFee = "8"
    mockSep31Refunds.refundPayments = mockRefundPaymentList

    // mock the Sep31GetTransactionResponse.Sep31RefundPayment we want
    val wantSep31GetTransactionResponseRefunds =
      Sep31GetTransactionResponse.Refunds.builder()
        .amountRefunded("100")
        .amountFee("8")
        .payments(
          listOf(
            Sep31GetTransactionResponse.Sep31RefundPayment.builder()
              .id("A")
              .amount("50")
              .fee("4")
              .build(),
            Sep31GetTransactionResponse.Sep31RefundPayment.builder()
              .id("B")
              .amount("50")
              .fee("4")
              .build()
          )
        )
        .build()

    // build the Sep31GetTransactionResponse.Sep31RefundPayment object
    val gotSep31GetTransactionResponseRefunds = mockSep31Refunds.toSep31TransactionResponseRefunds()
    assertEquals(wantSep31GetTransactionResponseRefunds, gotSep31GetTransactionResponseRefunds)
  }

  @Test
  fun `test conversion to CallbackApi Refund`() {
    // mock the SEP-31 Refunds object
    val mockRefundPayment1 = PojoSep31RefundPayment()
    mockRefundPayment1.id = "A"
    mockRefundPayment1.amount = "50"
    mockRefundPayment1.fee = "4"
    val mockRefundPayment2 = PojoSep31RefundPayment()
    mockRefundPayment2.id = "B"
    mockRefundPayment2.amount = "50"
    mockRefundPayment2.fee = "4"
    val mockRefundPaymentList = listOf(mockRefundPayment1, mockRefundPayment2)
    val mockSep31Refunds = PojoSep31Refunds()
    mockSep31Refunds.amountRefunded = "100"
    mockSep31Refunds.amountFee = "8"
    mockSep31Refunds.refundPayments = mockRefundPaymentList

    // mock the PlatformApi Refund we want
    val wantPlatformApiRefund =
      org.stellar.anchor.api.shared.Refund.builder()
        .amountRefunded(Amount("100", stellarUSDC))
        .amountFee(Amount("8", stellarUSDC))
        .payments(
          arrayOf(
            RefundPayment.builder()
              .id("A")
              .idType(RefundPayment.IdType.STELLAR)
              .amount(Amount("50", stellarUSDC))
              .fee(Amount("4", stellarUSDC))
              .refundedAt(null)
              .refundedAt(null)
              .build(),
            RefundPayment.builder()
              .id("B")
              .idType(RefundPayment.IdType.STELLAR)
              .amount(Amount("50", stellarUSDC))
              .fee(Amount("4", stellarUSDC))
              .refundedAt(null)
              .refundedAt(null)
              .build()
          )
        )
        .build()

    // build the SEP-31 Refunds object
    val gotProtocolApiRefund = mockSep31Refunds.toPlatformApiRefund(stellarUSDC)
    assertEquals(wantPlatformApiRefund, gotProtocolApiRefund)
  }

  @Test
  fun `test CallbackApi Refund creation`() {
    // mock the CallbackApi Refund
    val mockPlatformApiRefund =
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

    // mock the SEP-31 Refunds object we want
    val wantRefundPayment1 = PojoSep31RefundPayment()
    wantRefundPayment1.id = "A"
    wantRefundPayment1.amount = "50"
    wantRefundPayment1.fee = "4"
    val wantRefundPayment2 = PojoSep31RefundPayment()
    wantRefundPayment2.id = "B"
    wantRefundPayment2.amount = "50"
    wantRefundPayment2.fee = "4"
    val wantRefundPaymentList = listOf(wantRefundPayment1, wantRefundPayment2)
    val wantSep31Refunds = PojoSep31Refunds()
    wantSep31Refunds.amountRefunded = "100"
    wantSep31Refunds.amountFee = "8"
    wantSep31Refunds.refundPayments = wantRefundPaymentList

    // build the SEP-31 Refunds object
    val gotSep31Refunds = Refunds.of(mockPlatformApiRefund, sep31TransactionStore)
    assertEquals(wantSep31Refunds, gotSep31Refunds)
  }
}
