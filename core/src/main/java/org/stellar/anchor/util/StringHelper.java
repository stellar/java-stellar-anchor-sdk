package org.stellar.anchor.util;

import java.util.Objects;

public class StringHelper {
  public static boolean isEmpty(String value) {
    return Objects.toString(value, "").isEmpty();
  }

  public static boolean isNotEmpty(String value) {
    return !isEmpty(value);
  }

  /**
   * Convert camelCase string to an under-scored snake-case string.
   *
   * @param camel the camel case string.
   * @return under-scored snake-case string
   */
  public static String camelToSnake(String camel) {
    return camel
        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
        .replaceAll("([a-z])([A-Z])", "$1_$2")
        .replaceAll("-", "_")
        .toLowerCase();
  }

  public static String toPosixForm(String camel) {
    return camel
        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
        .replaceAll("([a-z])([A-Z])", "$1_$2")
        .replaceAll("-", "_")
        .replaceAll("\\.", "_")
        .toUpperCase();
  }
}
