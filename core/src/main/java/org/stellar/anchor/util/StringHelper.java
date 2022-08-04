package org.stellar.anchor.util;

import java.util.Objects;

public class StringHelper {
  public static boolean isEmpty(String value) {
    return Objects.toString(value, "").isEmpty();
  }
}
