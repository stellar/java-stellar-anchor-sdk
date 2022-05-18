package org.stellar.anchor.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.stellar.anchor.api.sep.AssetInfo;

public class MathHelper {
  public static BigDecimal decimal(String value) {
    if (value == null) return null;
    return new BigDecimal(value);
  }

  public static BigDecimal decimal(String value, AssetInfo asset) {
    if (value == null) return null;
    return new BigDecimal(value).setScale(asset.getSignificantDecimals(), RoundingMode.HALF_DOWN);
  }

  public static BigDecimal decimal(Long value) {
    if (value == null) return null;
    return BigDecimal.valueOf(value);
  }
}
