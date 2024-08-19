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

    // Check for duplicate assets
    Set<String> existingAssetNames = new HashSet<>();
    for (AssetInfo asset : assetService.listAllAssets()) {
      if (asset != null && !existingAssetNames.add(asset.getSep38AssetName())) {
        throw new InvalidConfigException(
            "Duplicate assets defined in configuration. Asset = " + asset.getSep38AssetName());
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
    AssetInfo.DepositWithdrawInfo sep6Info = assetInfo.getSep6();
    if (assetInfo.getIsServiceEnabled(sep6Info, "withdraw")) {
      // Check for missing SEP-6 withdrawal types
      if (isEmpty(sep6Info.getWithdraw().getMethods())) {
        throw new InvalidConfigException(
            "Withdraw types not defined for asset " + assetInfo.getSep38AssetName());
      }

      // Check for duplicate SEP-6 withdrawal types
      Set<String> existingWithdrawTypes = new HashSet<>();
      for (String type : sep6Info.getWithdraw().getMethods()) {
        if (!existingWithdrawTypes.add(type)) {
          throw new InvalidConfigException(
              "Duplicate withdraw types defined for asset "
                  + assetInfo.getSep38AssetName()
                  + ". Type = "
                  + type);
        }
      }
    }
  }

  private static void validateDeposit(AssetInfo assetInfo) throws InvalidConfigException {
    // Validate deposit fields
    AssetInfo.DepositWithdrawInfo sep6Info = assetInfo.getSep6();
    if (assetInfo.getIsServiceEnabled(sep6Info, "deposit")) {
      // Check for missing SEP-6 deposit types
      if (isEmpty(sep6Info.getDeposit().getMethods())) {
        throw new InvalidConfigException(
            "Deposit types not defined for asset " + assetInfo.getSep38AssetName());
      }

      // Check for duplicate SEP-6 deposit types
      Set<String> existingDepositTypes = new HashSet<>();
      for (String type : sep6Info.getDeposit().getMethods()) {
        if (!existingDepositTypes.add(type)) {
          throw new InvalidConfigException(
              "Duplicate deposit types defined for asset "
                  + assetInfo.getSep38AssetName()
                  + ". Type = "
                  + type);
        }
      }
    }
  }
}
