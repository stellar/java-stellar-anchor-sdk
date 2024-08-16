package org.stellar.anchor.util;

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
}
