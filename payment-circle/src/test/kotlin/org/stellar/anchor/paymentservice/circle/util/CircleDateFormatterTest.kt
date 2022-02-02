package org.stellar.anchor.paymentservice.circle.util

import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*
import org.junit.jupiter.api.Assertions.*

internal class CircleDateFormatterTest {
    @Test
    fun testDateFormatterField() {
        val wantDateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        wantDateFormatter.timeZone = TimeZone.getTimeZone("UTC")
        assertEquals(wantDateFormatter, org.stellar.anchor.paymentservice.circle.util.CircleDateFormatter.dateFormatter)
    }

    @Test
    fun testDateToString() {
        val date = Date(1640998861544)
        val gotDateStr = org.stellar.anchor.paymentservice.circle.util.CircleDateFormatter.dateToString(Date.from(date.toInstant()))
        val wantDateStr = "2022-01-01T01:01:01.544Z"
        assertEquals(wantDateStr, gotDateStr)
    }

    @Test
    fun testStringToDate() {
        val gotDate = org.stellar.anchor.paymentservice.circle.util.CircleDateFormatter.stringToDate("2022-01-01T01:01:01.544Z")
        val wantDate = Date(1640998861544)
        assertEquals(wantDate, gotDate)
    }
}
