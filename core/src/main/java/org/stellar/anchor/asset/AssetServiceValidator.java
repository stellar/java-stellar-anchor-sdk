package org.stellar.anchor.asset;

import static org.stellar.anchor.util.ListHelper.isEmpty;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.stellar.anchor.api.asset.*;
import org.stellar.anchor.api.exception.InvalidConfigException;

@RequiredArgsConstructor
public class AssetServiceValidator {

  public static void validate(AssetService assetService) throws InvalidConfigException {
    // Check for non-zero assets
    if (assetService == null || isEmpty(assetService.getAssets())) {
      throw new InvalidConfigException("0 assets defined in configuration");
    }

    // Check for duplicate assets
    Set<String> existingAssetNames = new HashSet<>();
    for (AssetInfo asset : assetService.getAssets()) {
      if (asset != null && !existingAssetNames.add(asset.getId())) {
        throw new InvalidConfigException(
            "Duplicate assets defined in configuration. Asset = " + asset.getId());
      }
    }

    // Validate stellar assets
    for (StellarAssetInfo stellarAsset : assetService.getStellarAssets()) {
      validateStellarAsset(stellarAsset);
    }

    // Validate fiat assets
    for (FiatAssetInfo fiatAsset : assetService.getFiatAssets()) {
      validateFiatAsset(fiatAsset);
    }
  }

  // Check if deposit is enabled for the asset
  public static boolean isDepositEnabled(DepositWithdrawInfo info) {
    if (info == null || !info.getEnabled()) {
      return false;
    }
    DepositWithdrawOperation operation = info.getDeposit();
    return operation != null && operation.getEnabled();
  }

  // Check if withdrawal is enabled for the asset
  public static boolean isWithdrawEnabled(DepositWithdrawInfo info) {
    if (info == null || !info.getEnabled()) {
      return false;
    }
    DepositWithdrawOperation operation = info.getWithdraw();
    return operation != null && operation.getEnabled();
  }

  private static void validateStellarAsset(StellarAssetInfo assetInfo)
      throws InvalidConfigException {
    // Check for missing distribution account field
    if (assetInfo.getDistributionAccount() == null) {
      throw new InvalidConfigException(
          "distribution_account not defined for asset " + assetInfo.getId());
    }
    // Check for missing significant decimals field
    if (assetInfo.getSignificantDecimals() == null) {
      throw new InvalidConfigException(
          "significant_decimals not defined for asset " + assetInfo.getId());
    }

    validateSep6(assetInfo);
    validateSep31(assetInfo);
    // TODO: ANCHOR-374 add more validation

  }

  private static void validateFiatAsset(FiatAssetInfo fiatAsset) throws InvalidConfigException {
    // TODO: ANCHOR-374 add more validation

  }

  private static void validateSep31(StellarAssetInfo assetInfo) throws InvalidConfigException {
    // Validate SEP-31 fields
    if (assetInfo.getSep31() != null && assetInfo.getSep31().getEnabled()) {
      boolean isQuotesSupported = assetInfo.getSep31().isQuotesSupported();
      boolean isQuotesRequired = assetInfo.getSep31().isQuotesRequired();
      if (isQuotesRequired && !isQuotesSupported) {
        throw new InvalidConfigException(
            "if quotes_required is true, quotes_supported must also be true for asset: "
                + assetInfo.getId());
      }
    }
  }

  private static void validateSep6(StellarAssetInfo assetInfo) throws InvalidConfigException {
    // Validate withdraw fields
    DepositWithdrawInfo dwInfo = assetInfo.getSep6();
    if (isWithdrawEnabled(dwInfo)) {
      // Check for missing SEP-6 withdrawal types
      if (isEmpty(dwInfo.getWithdraw().getMethods())) {
        throw new InvalidConfigException(
            "Withdraw types not defined for asset " + assetInfo.getId());
      }

      // Check for duplicate SEP-6 withdrawal types
      Set<String> existingWithdrawTypes = new HashSet<>();
      for (String type : dwInfo.getWithdraw().getMethods()) {
        if (!existingWithdrawTypes.add(type)) {
          throw new InvalidConfigException(
              "Duplicate withdraw types defined for asset "
                  + assetInfo.getId()
                  + ". Type = "
                  + type);
        }
      }
    }

    // Validate deposit fields
    if (isDepositEnabled(dwInfo)) {
      // Check for missing SEP-6 deposit types
      if (isEmpty(dwInfo.getDeposit().getMethods())) {
        throw new InvalidConfigException(
            "Deposit types not defined for asset " + assetInfo.getId());
      }

      // Check for duplicate SEP-6 deposit types
      Set<String> existingDepositTypes = new HashSet<>();
      for (String type : dwInfo.getDeposit().getMethods()) {
        if (!existingDepositTypes.add(type)) {
          throw new InvalidConfigException(
              "Duplicate deposit types defined for asset "
                  + assetInfo.getId()
                  + ". Type = "
                  + type);
        }
      }
    }
  }
}
