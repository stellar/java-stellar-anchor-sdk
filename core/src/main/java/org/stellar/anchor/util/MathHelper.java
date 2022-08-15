package org.stellar.anchor.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import org.stellar.anchor.api.sep.AssetInfo;

public class MathHelper {
  public static BigDecimal decimal(String value) {
    if (value == null) return null;
    return new BigDecimal(value);
  }

  public static BigDecimal decimal(String value, int scale) {
    if (value == null) return null;
    return new BigDecimal(value).setScale(scale, RoundingMode.HALF_DOWN);
  }

  public static BigDecimal decimal(String value, AssetInfo asset) {
    if (value == null) return null;
    return decimal(value, asset.getSignificantDecimals());
  }

  public static BigDecimal decimal(Long value) {
    if (value == null) return null;
    return BigDecimal.valueOf(value);
  }

  public static boolean equalsAsDecimals(String valueA, String valueB) {
    if (valueA == null && valueB == null) {
      return true;
    } else if (valueA == null || valueB == null) {
      return false;
    }
    return decimal(valueA).compareTo(decimal(valueB)) == 0;
  }

  public static String formatAmount(BigDecimal amount, Integer decimals) {
    BigDecimal newAmount = amount.setScale(decimals, RoundingMode.HALF_DOWN);

    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(decimals);
    df.setMinimumFractionDigits(0);
    df.setGroupingUsed(false);

    return df.format(newAmount);
  }

  public static String formatAmount(BigDecimal amount) {
    return formatAmount(amount, 4);
  }
}
