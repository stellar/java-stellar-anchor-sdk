package org.stellar.anchor.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.format.DateTimeParseException

internal class DateUtilTest {
    @Test
    fun testISO8601() {
        val t1 = System.currentTimeMillis() / 1000
        val t2 = DateUtil.fromISO8601UTC(DateUtil.toISO8601UTC(t1))
        assert(t1 == t2)
    }

    @Test
    fun testISO8601Exception() {
        Assertions.assertThrows(DateTimeParseException::class.java) { DateUtil.fromISO8601UTC("12345") }
    }
}