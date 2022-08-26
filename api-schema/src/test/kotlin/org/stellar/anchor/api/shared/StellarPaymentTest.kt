package org.stellar.anchor.api.shared

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StellarPaymentTest {
  companion object {
    private const val stellarUSDC =
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
  }

  @Test
  fun `test addOrUpdatePayments with empty payment list`() {
    var paymentList: List<StellarPayment>? = null

    paymentList =
      StellarPayment.addOrUpdatePayments(
        paymentList,
        StellarPayment.builder()
          .id("4609238642995201")
          .amount(Amount("100.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build()
      )

    val wantPaymentList =
      listOf(
        StellarPayment.builder()
          .id("4609238642995201")
          .amount(Amount("100.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build()
      )

    assertEquals(wantPaymentList, paymentList)
  }

  @Test
  fun `test addOrUpdatePayments with existing identical payment`() {
    var paymentList: List<StellarPayment>? =
      listOf(
        StellarPayment.builder()
          .id("4609238642995201")
          .amount(Amount("100.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build()
      )

    paymentList =
      StellarPayment.addOrUpdatePayments(
        paymentList,
        StellarPayment.builder()
          .id("4609238642995201")
          .amount(Amount("100.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build()
      )

    val wantPaymentList =
      listOf(
        StellarPayment.builder()
          .id("4609238642995201")
          .amount(Amount("100.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build()
      )

    assertEquals(wantPaymentList, paymentList)
  }

  @Test
  fun `test addOrUpdatePayments updating existing payment`() {
    var paymentList: List<StellarPayment>? =
      listOf(StellarPayment.builder().id("4609238642995201").build())

    paymentList =
      StellarPayment.addOrUpdatePayments(
        paymentList,
        StellarPayment.builder()
          .id("4609238642995201")
          .amount(Amount("100.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build()
      )

    val wantPaymentList =
      listOf(
        StellarPayment.builder()
          .id("4609238642995201")
          .amount(Amount("100.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build()
      )

    assertEquals(wantPaymentList, paymentList)
  }

  @Test
  fun `test addOrUpdatePayments adding 2 payments`() {
    var paymentList: List<StellarPayment>? =
      listOf(
        StellarPayment.builder()
          .id("1111")
          .amount(Amount("100.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build()
      )

    paymentList =
      StellarPayment.addOrUpdatePayments(
        paymentList,
        StellarPayment.builder()
          .id("2222")
          .amount(Amount("200.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build(),
        StellarPayment.builder()
          .id("3333")
          .amount(Amount("300.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build()
      )

    val wantPaymentList =
      listOf(
        StellarPayment.builder()
          .id("1111")
          .amount(Amount("100.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build(),
        StellarPayment.builder()
          .id("2222")
          .amount(Amount("200.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build(),
        StellarPayment.builder()
          .id("3333")
          .amount(Amount("300.0000", stellarUSDC))
          .paymentType(StellarPayment.Type.PAYMENT)
          .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
          .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
          .build()
      )

    assertEquals(wantPaymentList, paymentList)
  }
}
