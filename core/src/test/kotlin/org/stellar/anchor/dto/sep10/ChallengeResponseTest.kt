package org.stellar.anchor.dto.sep10

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ChallengeResponseTest {
    companion object {
        const val TEST_TRANSACTION = "TEST TXN"
        const val TEST_NETWORKPHRASE = "Test SDF Network ; September 2015"
    }

    @Test
    fun testOf() {
        val cr = ChallengeResponse.of(TEST_TRANSACTION, TEST_NETWORKPHRASE)
        assertEquals(TEST_TRANSACTION, cr.transaction)
        assertEquals(TEST_NETWORKPHRASE, cr.networkPassphrase)
    }
}
