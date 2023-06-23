package org.stellar.anchor.util;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.util.Currency;
import org.stellar.sdk.KeyPair;

public class AssetHelper {
  public static boolean isISO4217(String assetCode, String assetIssuer) {
    // assetIssuer must be empty to be a valid Fiat
    if (!isEmpty(assetIssuer)) {
      return false;
    }
    try {
      // check if assetCode is ISO4217 compliant
      Currency.getInstance(assetCode);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  /**
   * Checks if the asset is a non-native issued asset.
   *
   * @param assetCode The asset code
   * @param assetIssuer The asset issuer
   * @return true if the asset is a non-native issued asset.
   */
  public static boolean isNonNativeAsset(String assetCode, String assetIssuer) {
    if (isEmpty(assetCode)) return false;
    if (isEmpty(assetIssuer)) return false;
    // check if assetIssuer is a valid wallet address
    try {
      KeyPair.fromAccountId(assetIssuer);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  /**
   * Checks if the asset is native (XLM). Native assets have an empty assetIssuer.
   *
   * @param assetCode The asset code
   * @param assetIssuer The asset issuer
   * @return true if the asset is native (XLM)
   */
  public static boolean isNativeAsset(String assetCode, String assetIssuer) {
    return "native".equals(assetCode) && isEmpty(assetIssuer);
  }

  /**
   * Returns the asset id in the SEP-38 asset identification format, or null if the asset is not
   * supported.
   *
   * @param assetCode The asset code
   * @param assetIssuer The asset issuer
   * @return The asset id in the SEP-38 asset identification format.
   */
  public static String getAssetId(String assetCode, String assetIssuer) {
    if (isISO4217(assetCode, assetIssuer)) {
      // fiat assets
      return "iso4217:" + assetCode;
    } else if (isNativeAsset(assetCode, assetIssuer)) {
      return "stellar:native";
    } else if (isNonNativeAsset(assetCode, assetIssuer)) {
      return "stellar:" + assetCode + ":" + assetIssuer;
    } else {
      // not supported
      return null;
    }
  }
}
