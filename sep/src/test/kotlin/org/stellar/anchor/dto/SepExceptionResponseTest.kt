package org.stellar.anchor.dto

import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.SepExceptionResponse

internal class SepExceptionResponseTest {
  @Test
  fun test() {
    val ser = SepExceptionResponse("")
    ser.getError()
    ser.setError("")
  }
}
