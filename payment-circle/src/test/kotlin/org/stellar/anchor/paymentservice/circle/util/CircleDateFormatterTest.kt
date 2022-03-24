package org.stellar.anchor.paymentservice.circle.util

import java.time.ZoneId
import java.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CircleDateFormatterTest {
  @Test
  fun testDateFormatterField() {
    val wantZoneId = ZoneId.of("UTC")
    assertEquals(wantZoneId, CircleDateFormatter.dateFormatter.zone)
  }

  @Test
  fun testDateToString() {
    val date = Date(1640998861544)
    val gotDateStr = CircleDateFormatter.dateToString(Date.from(date.toInstant()))
    val wantDateStr = "2022-01-01T01:01:01.544Z"
    assertEquals(wantDateStr, gotDateStr)
  }

  @Test
  fun testStringToDate() {
    val gotDate = CircleDateFormatter.stringToDate("2022-01-01T01:01:01.544Z")
    val wantDate = Date(1640998861544)
    assertEquals(wantDate, gotDate)
  }
}
