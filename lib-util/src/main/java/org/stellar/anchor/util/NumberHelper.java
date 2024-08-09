package org.stellar.anchor.util;

public class NumberHelper {
  public static boolean isPositiveNumber(String str) {
    if (str == null) {
      return false;
    }

    try {
      double number = Double.parseDouble(str);
      return number > 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
