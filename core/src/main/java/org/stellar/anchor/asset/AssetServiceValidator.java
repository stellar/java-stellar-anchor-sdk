package org.stellar.anchor.asset;

import static org.stellar.anchor.util.ListHelper.isEmpty;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.sep.AssetInfo;

@RequiredArgsConstructor
public class AssetServiceValidator {

  public static void validate(AssetService assetService) throws InvalidConfigException {
    // Check for non-zero assets
    if (assetService == null || isEmpty(assetService.listAllAssets())) {
      throw new InvalidConfigException("0 assets defined in configuration");
    }

    // TODO: remove this check once we support SEP-31 and SEP-38 for native asset
    AssetInfo nativeAsset = assetService.getAsset("native");
    if (nativeAsset != null && (nativeAsset.getSep31Enabled() || nativeAsset.getSep38Enabled())) {
      throw new InvalidConfigException("Native asset does not support SEP-31 or SEP-38");
    }

    // Check for duplicate assets
    Set<String> existingAssetNames = new HashSet<>();
    for (AssetInfo asset : assetService.listAllAssets()) {
      if (asset != null && !existingAssetNames.add(asset.getAssetName())) {
        throw new InvalidConfigException(
            "Duplicate assets defined in configuration. Asset = " + asset.getAssetName());
      }
    }

    // Asset level validation
    for (AssetInfo assetInfo : assetService.listAllAssets()) {
      validateWithdraw(assetInfo);
      validateDeposit(assetInfo);
      // TODO: ANCHOR-374 add more validation
    }
  }

  private static void validateWithdraw(AssetInfo assetInfo) throws InvalidConfigException {
    // Validate withdraw fields
    if (assetInfo.getSep6Enabled()) {
      if (assetInfo.getWithdraw() != null && assetInfo.getWithdraw().getEnabled()) {

        // Check for missing SEP-6 withdrawal types
        if (isEmpty(assetInfo.getWithdraw().getMethods())) {
          throw new InvalidConfigException(
              "Withdraw types not defined for asset " + assetInfo.getAssetName());
        }

        // Check for duplicate SEP-6 withdrawal types
        Set<String> existingWithdrawTypes = new HashSet<>();
        for (String type : assetInfo.getWithdraw().getMethods()) {
          if (!existingWithdrawTypes.add(type)) {
            throw new InvalidConfigException(
                "Duplicate withdraw types defined for asset "
                    + assetInfo.getAssetName()
                    + ". Type = "
                    + type);
          }
        }
      }
    }
  }

  private static void validateDeposit(AssetInfo assetInfo) throws InvalidConfigException {
    // Validate deposit fields
    if (assetInfo.getSep6Enabled()) {
      if (assetInfo.getDeposit() != null && assetInfo.getDeposit().getEnabled()) {

        // Check for missing SEP-6 deposit types
        if (isEmpty(assetInfo.getDeposit().getMethods())) {
          throw new InvalidConfigException(
              "Deposit types not defined for asset " + assetInfo.getAssetName());
        }

        // Check for duplicate SEP-6 deposit types
        Set<String> existingDepositTypes = new HashSet<>();
        for (String type : assetInfo.getDeposit().getMethods()) {
          if (!existingDepositTypes.add(type)) {
            throw new InvalidConfigException(
                "Duplicate deposit types defined for asset "
                    + assetInfo.getAssetName()
                    + ". Type = "
                    + type);
          }
        }
      }
    }
  }
}
