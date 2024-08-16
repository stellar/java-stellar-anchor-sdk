package org.stellar.anchor.util;

import static java.lang.Math.*;

import java.math.BigDecimal;

public class NumberHelper {
  public static boolean isPositiveNumber(String str) {
    if (str == null) {
      return false;
    }

    try {
      return new BigDecimal(str).compareTo(BigDecimal.ZERO) > 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static boolean hasProperSignificantDecimals(
      String input, int minDecimals, int maxDecimals) {
    try {
      BigDecimal decimal = new BigDecimal(input);
      int scale = max(0, decimal.stripTrailingZeros().scale());

      return scale >= minDecimals && scale <= maxDecimals;
    } catch (NumberFormatException e) {
      // If the input is not a valid number, return false
      return false;
    }
  }
}
