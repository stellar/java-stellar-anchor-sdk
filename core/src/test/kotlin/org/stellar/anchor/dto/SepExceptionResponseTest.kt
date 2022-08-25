package org.stellar.anchor.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.SepExceptionResponse

internal class SepExceptionResponseTest {
  @Test
  fun `test SepExceptionResponse creation`() {
    val ser = SepExceptionResponse("Test Error Message")
    assertEquals("Test Error Message", ser.getError())
  }
}
