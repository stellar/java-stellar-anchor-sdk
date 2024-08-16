package org.stellar.anchor.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

class NumberHelperTest {
  @ParameterizedTest
  @ValueSource(strings = ["1", "100", "100000", "100.000"])
  fun `test positive numbers`(value: String) {
    assert(NumberHelper.isPositiveNumber(value))
  }

  @ParameterizedTest
  @ValueSource(strings = ["0", "-1", "-100", "-100000", "-100.000"])
  @NullSource
  fun `test non positive numbers`(value: String?) {
    assertFalse(NumberHelper.isPositiveNumber(value))
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        "1, 0, 2",
        "-1, 0, 2",
        "1.00, 0, 2",
        "100.00, 0, 2",
        "101.00, 0, 2",
        "100.000000, 0, 2",
        "101.000000, 0, 2",
        "-1.00, 0, 2",
        "01.00, 0, 2",
        "1.000, 0, 2",
        "1.01, 2, 4",
        "1.010000, 2, 4",
        "1.0001, 2, 4",
      ]
  )
  fun `test proper significant decimals`(value: String, minDecimals: Int, maxDecimals: Int) {
    assert(NumberHelper.hasProperSignificantDecimals(value, minDecimals, maxDecimals))
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        "1.001, 0, 2",
        "-1.001, 0, 2",
        "1.0, 2, 4",
        "-1.0, 2, 4",
        "1.000, 2, 4",
        "-1.000, 2, 4",
        "1.0000, 2, 4",
        "-1.0000, 2, 4",
        "1.00000, 2, 4",
        "1.00001, 2, 4",
        "a, 1, 2",
      ]
  )
  fun `test violating significant decimals`(value: String, minDecimals: Int, maxDecimals: Int) {
    assert(!NumberHelper.hasProperSignificantDecimals(value, minDecimals, maxDecimals))
  }
}
