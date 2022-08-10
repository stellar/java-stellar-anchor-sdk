package org.stellar.anchor.util

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MathHelperTest {
  @ParameterizedTest
  @CsvSource(
    value = ["0,0", "0.1,0.1000000", "1,1", "1,1.000000", "-1,-1", "-1,-1.0000000", "500,5e2"]
  )
  fun test_equalsAsDecimals_true(valueA: String, valueB: String) {
    assertTrue(MathHelper.equalsAsDecimals(valueA, valueB))
  }

  @ParameterizedTest
  @CsvSource(value = ["0,-0.0000001", "-0.1,0.1000000", "0,1.000000"])
  fun test_equalsAsDecimals_false(valueA: String, valueB: String) {
    assertFalse(MathHelper.equalsAsDecimals(valueA, valueB))
  }
}
