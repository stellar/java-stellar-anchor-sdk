package org.stellar.anchor.util

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MathHelperTest {
  @ParameterizedTest
  @CsvSource(
    value = [",", "0,0", "0.1,0.1000000", "1,1", "1,1.000000", "-1,-1", "-1,-1.0000000", "500,5e2"]
  )
  fun `test equalsAsDecimals true`(valueA: String?, valueB: String?) {
    assertTrue(MathHelper.equalsAsDecimals(valueA, valueB))
  }

  @ParameterizedTest
  @CsvSource(value = ["0,-0.0000001", "-0.1,0.1000000", "0,1.000000", "0,", ",0"])
  fun `test equalsAsDecimals false`(valueA: String?, valueB: String?) {
    assertFalse(MathHelper.equalsAsDecimals(valueA, valueB))
  }

  @ParameterizedTest
  @CsvSource(value = ["a,a", "1,a", "a,1"])
  fun `test equalsAsDecimals throws`(valueA: String, valueB: String) {
    val ex: Exception = assertThrows { MathHelper.equalsAsDecimals(valueA, valueB) }
    assertInstanceOf(NumberFormatException::class.java, ex)
    assertEquals(
      "Character a is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark.",
      ex.message
    )
  }
}
