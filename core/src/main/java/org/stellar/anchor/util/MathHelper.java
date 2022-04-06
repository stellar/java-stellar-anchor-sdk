package org.stellar.anchor.util;

import java.math.BigDecimal;

public class MathHelper {
  public static BigDecimal decimal(String value) {
    return new BigDecimal(value);
  }

  public static BigDecimal decimal(Long value) {
    return new BigDecimal(value);
  }
}
