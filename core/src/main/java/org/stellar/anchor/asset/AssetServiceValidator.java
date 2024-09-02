package org.stellar.anchor.asset;

import static org.stellar.anchor.util.ListHelper.isEmpty;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.stellar.anchor.api.asset.AssetInfo;
import org.stellar.anchor.api.asset.DepositWithdrawInfo;
import org.stellar.anchor.api.asset.FiatAssetInfo;
import org.stellar.anchor.api.asset.StellarAssetInfo;
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

    // Validate stellar asset
    for (StellarAssetInfo assetInfo : assetService.getStellarAssets()) {
      validateWithdraw(assetInfo);
      validateDeposit(assetInfo);
      validateQuoteSupportedIfRequired(assetInfo);
      // TODO: ANCHOR-374 add more validation
    }

    for (FiatAssetInfo assetInfo : assetService.getFiatAssets()) {
      validateQuoteSupportedIfRequired(assetInfo);
      // TODO: ANCHOR-374 add more validation
    }
  }

  private static void validateQuoteSupportedIfRequired(AssetInfo assetInfo)
      throws InvalidConfigException {
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

  private static void validateWithdraw(StellarAssetInfo assetInfo) throws InvalidConfigException {
    // Validate withdraw fields
    DepositWithdrawInfo sep6Info = assetInfo.getSep6();
    if (assetInfo.isWithdrawEnabled(sep6Info)) {
      // Check for missing SEP-6 withdrawal types
      if (isEmpty(sep6Info.getWithdraw().getMethods())) {
        throw new InvalidConfigException(
            "Withdraw types not defined for asset " + assetInfo.getId());
      }

      // Check for duplicate SEP-6 withdrawal types
      Set<String> existingWithdrawTypes = new HashSet<>();
      for (String type : sep6Info.getWithdraw().getMethods()) {
        if (!existingWithdrawTypes.add(type)) {
          throw new InvalidConfigException(
              "Duplicate withdraw types defined for asset "
                  + assetInfo.getId()
                  + ". Type = "
                  + type);
        }
      }
    }
  }

  private static void validateDeposit(StellarAssetInfo assetInfo) throws InvalidConfigException {
    // Validate deposit fields
    DepositWithdrawInfo sep6Info = assetInfo.getSep6();
    if (assetInfo.isDepositEnabled(sep6Info)) {
      // Check for missing SEP-6 deposit types
      if (isEmpty(sep6Info.getDeposit().getMethods())) {
        throw new InvalidConfigException(
            "Deposit types not defined for asset " + assetInfo.getId());
      }

      // Check for duplicate SEP-6 deposit types
      Set<String> existingDepositTypes = new HashSet<>();
      for (String type : sep6Info.getDeposit().getMethods()) {
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
