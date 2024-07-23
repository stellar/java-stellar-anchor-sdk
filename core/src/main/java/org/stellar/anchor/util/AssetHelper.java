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
   * Returns the asset code from asset
   *
   * @param asset asset
   * @return The asset code
   */
  public static String getAssetCode(String asset) {
    return asset.split(":")[1];
  }

  /**
   * Returns the asset issuer from asset
   *
   * @param asset asset
   * @return The asset issuer. If issuer is absent, then returns NULL
   */
  public static String getAssetIssuer(String asset) {
    return asset.split(":").length >= 3 ? asset.split(":")[2] : null;
  }
}
