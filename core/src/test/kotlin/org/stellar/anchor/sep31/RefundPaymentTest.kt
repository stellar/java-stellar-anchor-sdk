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

class RefundPaymentTest {
  companion object {
    private const val stellarUSDC =
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
  }

  @MockK(relaxed = true) private lateinit var sep31TransactionStore: Sep31TransactionStore

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { sep31TransactionStore.newRefundPayment() } answers { PojoSep31RefundPayment() }
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `test to sep31 refund payment`() {
    // mock the SEP-31 RefundPayment object
    val mockRefundPayment = PojoSep31RefundPayment()
    mockRefundPayment.id = "A"
    mockRefundPayment.amount = "50"
    mockRefundPayment.fee = "4"

    // mock the Sep31GetTransactionResponse.Sep31RefundPayment object we want
    val wantSep31RefundPayment =
      Sep31GetTransactionResponse.Sep31RefundPayment.builder().id("A").amount("50").fee("4").build()

    // build the Sep31GetTransactionResponse.Sep31RefundPayment object
    val gotSep31RefundPayment = mockRefundPayment.toSep31RefundPayment()
    assertEquals(wantSep31RefundPayment, gotSep31RefundPayment)
  }

  @Test
  fun `test to platform api refund payment`() {
    // mock the SEP-31 RefundPayment object
    val mockRefundPayment = PojoSep31RefundPayment()
    mockRefundPayment.id = "A"
    mockRefundPayment.amount = "50"
    mockRefundPayment.fee = "4"

    // mock the PlatformApi RefundPayment object we want
    val wantPlatformApiRefundPayment =
      org.stellar.anchor.api.shared.RefundPayment.builder()
        .id("A")
        .idType(org.stellar.anchor.api.shared.RefundPayment.IdType.STELLAR)
        .amount(Amount("50", stellarUSDC))
        .fee(Amount("4", stellarUSDC))
        .refundedAt(null)
        .refundedAt(null)
        .build()

    // build the SEP-31 RefundPayment object
    val gotPlatformApiRefundPayment = mockRefundPayment.toPlatformApiRefundPayment(stellarUSDC)
    assertEquals(wantPlatformApiRefundPayment, gotPlatformApiRefundPayment)
  }

  @Test
  fun `test PlatformApi Refund object creation`() {
    // mock the PlatformApi RefundPayment object
    val mockPlatformApiRefundPayment =
      org.stellar.anchor.api.shared.RefundPayment.builder()
        .id("A")
        .idType(org.stellar.anchor.api.shared.RefundPayment.IdType.STELLAR)
        .amount(Amount("50", stellarUSDC))
        .fee(Amount("4", stellarUSDC))
        .refundedAt(Instant.now())
        .refundedAt(Instant.now())
        .build()

    // mock the SEP-31 RefundPayment object we want
    val wantRefundPayment = PojoSep31RefundPayment()
    wantRefundPayment.id = "A"
    wantRefundPayment.amount = "50"
    wantRefundPayment.fee = "4"

    // build the SEP-31 RefundPayment object
    val gotSep31RefundPayment =
      RefundPayment.of(mockPlatformApiRefundPayment, sep31TransactionStore)
    assertEquals(wantRefundPayment, gotSep31RefundPayment)
  }
}
