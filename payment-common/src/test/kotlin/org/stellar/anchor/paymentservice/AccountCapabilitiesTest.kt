package org.stellar.anchor.paymentservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class AccountCapabilitiesTest {
  @Test
  fun testEquals() {
    var capabilities1 = org.stellar.anchor.paymentservice.Account.Capabilities()
    var capabilities2 = org.stellar.anchor.paymentservice.Account.Capabilities()
    assertEquals(capabilities1, capabilities2)

    capabilities1 = org.stellar.anchor.paymentservice.Account.Capabilities()
    capabilities2 =
      org.stellar.anchor.paymentservice.Account.Capabilities(
        org.stellar.anchor.paymentservice.PaymentNetwork.STELLAR
      )
    assertNotEquals(capabilities1, capabilities2)

    capabilities1 =
      org.stellar.anchor.paymentservice.Account.Capabilities(
        org.stellar.anchor.paymentservice.PaymentNetwork.CIRCLE
      )
    capabilities2 =
      org.stellar.anchor.paymentservice.Account.Capabilities(
        org.stellar.anchor.paymentservice.PaymentNetwork.STELLAR
      )
    assertNotEquals(capabilities1, capabilities2)

    capabilities1 =
      org.stellar.anchor.paymentservice.Account.Capabilities(
        org.stellar.anchor.paymentservice.PaymentNetwork.BANK_WIRE
      )
    capabilities2 =
      org.stellar.anchor.paymentservice.Account.Capabilities(
        org.stellar.anchor.paymentservice.PaymentNetwork.BANK_WIRE
      )
    assertEquals(capabilities1, capabilities2)
  }
}
