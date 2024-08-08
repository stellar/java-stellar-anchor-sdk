package org.stellar.anchor.util;

public class NumberHelper {
  public static boolean isValidPositiveNumber(String str) {
    if (str == null) {
      return false;
    }

    try {
      int number = Integer.parseInt(str);
      return number > 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
