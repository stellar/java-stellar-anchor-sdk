package org.stellar.anchor.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.stellar.anchor.asset.AssetInfo;

public class MathHelper {
  public static BigDecimal decimal(String value) {
    return new BigDecimal(value);
  }

  public static BigDecimal decimal(String value, AssetInfo asset) {
    return new BigDecimal(value).setScale(asset.getSignificantDecimals(), RoundingMode.HALF_DOWN);
  }

  public static BigDecimal decimal(Long value) {
    return new BigDecimal(value);
  }
}
