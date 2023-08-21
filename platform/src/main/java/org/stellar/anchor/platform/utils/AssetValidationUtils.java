package org.stellar.anchor.platform.utils;

import static java.util.stream.Collectors.toList;
import static org.stellar.anchor.util.MathHelper.decimal;

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.rpc.method.AmountAssetRequest;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.util.SepHelper;
import org.stellar.anchor.util.StringHelper;

public class AssetValidationUtils {

  private static final String STELLAR_ASSET_PREFIX = "stellar:";

  public static boolean isStellarAsset(String asset) {
    return asset.startsWith(STELLAR_ASSET_PREFIX);
  }

  /**
   * validateAsset will validate if the provided amount has valid values and if its asset is
   * supported.
   *
   * @param amount is the object containing the asset full name and the amount.
   */
  public static void validateAsset(
      String fieldName, AmountAssetRequest amount, AssetService assetService)
      throws BadRequestException {
    validateAsset(fieldName, amount, false, assetService);
  }

  public static void validateAsset(
      String fieldName, AmountAssetRequest amount, boolean allowZero, AssetService assetService)
      throws BadRequestException {
    if (amount == null) {
      return;
    }

    // asset amount needs to be non-empty and valid
    SepHelper.validateAmount(fieldName + ".", amount.getAmount(), allowZero);

    // asset name cannot be empty
    if (StringHelper.isEmpty(amount.getAsset())) {
      throw new BadRequestException(fieldName + ".asset cannot be empty");
    }

    List<AssetInfo> allAssets =
        assetService.listAllAssets().stream()
            .filter(assetInfo -> assetInfo.getAssetName().equals(amount.getAsset()))
            .collect(toList());

    // asset name needs to be supported
    if (CollectionUtils.isEmpty(allAssets)) {
      throw new BadRequestException(
          String.format("'%s' is not a supported asset.", amount.getAsset()));
    }

    if (allAssets.size() == 1) {
      AssetInfo targetAsset = allAssets.get(0);

      if (targetAsset.getSignificantDecimals() != null) {
        // Check that significant decimal is correct
        if (decimal(amount.getAmount(), targetAsset).compareTo(decimal(amount.getAmount())) != 0) {
          throw new BadRequestException(
              String.format(
                  "'%s' has invalid significant decimals. Expected: '%s'",
                  amount.getAmount(), targetAsset.getSignificantDecimals()));
        }
      }
    }
  }
}
