package org.stellar.anchor.paymentservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class AccountCapabilitiesTest {
    @Test
    fun testEquals() {
        var capabilities1 = Account.Capabilities()
        var capabilities2 = Account.Capabilities()
        assertEquals(capabilities1, capabilities2)

        capabilities1 = Account.Capabilities()
        capabilities2 = Account.Capabilities(Network.STELLAR)
        assertNotEquals(capabilities1, capabilities2)

        capabilities1 = Account.Capabilities(Network.CIRCLE)
        capabilities2 = Account.Capabilities(Network.STELLAR)
        assertNotEquals(capabilities1, capabilities2)

        capabilities1 = Account.Capabilities(Network.BANK_WIRE)
        capabilities2 = Account.Capabilities(Network.BANK_WIRE)
        assertEquals(capabilities1, capabilities2)
    }
}
