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

  public static boolean isIssuedAsset(String assetCode, String assetIssuer) {
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

  public static boolean isNativeAsset(String assetCode, String assetIssuer) {
    return "native".equals(assetCode) && isEmpty(assetIssuer);
  }

  public static String getAssetId(String assetCode, String assetIssuer) {
    if (isISO4217(assetCode, assetIssuer)) {
      // fiat assets
      return "iso4217:" + assetCode;
    } else if (isNativeAsset(assetCode, assetIssuer)) {
      return "stellar:native";
    } else if (isIssuedAsset(assetCode, assetIssuer)) {
      return "stellar:" + assetCode + ":" + assetIssuer;
    } else {
      // not supported
      return null;
    }
  }
}
