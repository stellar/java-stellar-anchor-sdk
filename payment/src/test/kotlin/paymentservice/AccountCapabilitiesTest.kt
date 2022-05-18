package paymentservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class AccountCapabilitiesTest {
  @Test
  fun testEquals() {
    var capabilities1 = paymentservice.Account.Capabilities()
    var capabilities2 = paymentservice.Account.Capabilities()
    assertEquals(capabilities1, capabilities2)

    capabilities1 = paymentservice.Account.Capabilities()
    capabilities2 = paymentservice.Account.Capabilities(paymentservice.PaymentNetwork.STELLAR)
    assertNotEquals(capabilities1, capabilities2)

    capabilities1 = paymentservice.Account.Capabilities(paymentservice.PaymentNetwork.CIRCLE)
    capabilities2 = paymentservice.Account.Capabilities(paymentservice.PaymentNetwork.STELLAR)
    assertNotEquals(capabilities1, capabilities2)

    capabilities1 = paymentservice.Account.Capabilities(paymentservice.PaymentNetwork.BANK_WIRE)
    capabilities2 = paymentservice.Account.Capabilities(paymentservice.PaymentNetwork.BANK_WIRE)
    assertEquals(capabilities1, capabilities2)
  }
}
