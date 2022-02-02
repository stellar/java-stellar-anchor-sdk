package org.stellar.anchor.dto

import org.junit.jupiter.api.Test

internal class SepExceptionResponseTest {
    @Test
    fun test() {
        val ser = SepExceptionResponse("")
        ser.getError()
        ser.setError("")
        ser.canEqual(Object())
    }

}
