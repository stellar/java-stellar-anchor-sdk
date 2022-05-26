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

  public static BigDecimal decimal(String value, AssetInfo asset) {
    if (value == null) return null;
    return new BigDecimal(value).setScale(asset.getSignificantDecimals(), RoundingMode.HALF_DOWN);
  }

  public static BigDecimal decimal(Long value) {
    if (value == null) return null;
    return BigDecimal.valueOf(value);
  }

  public static String formatAmount(BigDecimal amount, Integer decimals)
      throws NumberFormatException {
    BigDecimal newAmount = amount.setScale(decimals, RoundingMode.HALF_DOWN);

    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(decimals);
    df.setMinimumFractionDigits(0);
    df.setGroupingUsed(false);

    return df.format(newAmount);
  }

  public static String formatAmount(BigDecimal amount) throws NumberFormatException {
    return formatAmount(amount, 4);
  }
}
