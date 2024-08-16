package org.stellar.anchor.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.params.ParameterizedTest
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
}
