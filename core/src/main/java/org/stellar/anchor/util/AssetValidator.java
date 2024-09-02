package org.stellar.anchor.util;

import static org.stellar.anchor.api.asset.Sep31Info.*;
import static org.stellar.anchor.api.asset.Sep38Info.*;
import static org.stellar.anchor.util.ListHelper.isEmpty;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.stellar.anchor.api.asset.*;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.asset.AssetService;

@RequiredArgsConstructor
public class AssetValidator {
  private static final List<String> isoCountries = Arrays.asList(Locale.getISOCountries());

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
      validateStellarAsset(assetService, stellarAsset);
    }

    // Validate fiat assets
    for (FiatAssetInfo fiatAsset : assetService.getFiatAssets()) {
      validateFiatAsset(assetService, fiatAsset);
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

  private static void validateStellarAsset(
      AssetService assetService, StellarAssetInfo stallarAssetInfo) throws InvalidConfigException {
    // Check for missing significant decimals field
    if (stallarAssetInfo.getSignificantDecimals() == null) {
      throw new InvalidConfigException(
          "significant_decimals not defined for asset " + stallarAssetInfo.getId());
    }

    validateSep6(stallarAssetInfo.getSep6(), stallarAssetInfo.getId());
    validateSep24(stallarAssetInfo.getSep24(), stallarAssetInfo.getId());
    validateSep31(stallarAssetInfo.getSep31(), stallarAssetInfo.getId());
    validateSep38(assetService, stallarAssetInfo.getSep38(), stallarAssetInfo.getId());
  }

  private static void validateFiatAsset(AssetService assetService, FiatAssetInfo fiatAssetInfo)
      throws InvalidConfigException {
    validateSep31(fiatAssetInfo.getSep31(), fiatAssetInfo.getId());
    validateSep38(assetService, fiatAssetInfo.getSep38(), fiatAssetInfo.getId());
  }

  private static void validateSep6(Sep6Info sep6Info, String assetId)
      throws InvalidConfigException {
    // Validate SEP-6 fields
    if (sep6Info != null && sep6Info.getEnabled()) validateDepositWithdrawInfo(sep6Info, assetId);
  }

  private static void validateSep24(Sep24Info sep24Info, String assetId)
      throws InvalidConfigException {

    // Validate SEP-24 fields
    if (sep24Info != null && sep24Info.getEnabled())
      validateDepositWithdrawInfo(sep24Info, assetId);
  }

  private static void validateSep31(Sep31Info sep31Info, String assetId)
      throws InvalidConfigException {
    ReceiveOperation receiveInfo = sep31Info.getReceive();

    // Validate `quotes_required` and `quotes_supported` fields
    if (sep31Info.getEnabled()) {
      boolean isQuotesSupported = sep31Info.isQuotesSupported();
      boolean isQuotesRequired = sep31Info.isQuotesRequired();
      if (isQuotesRequired && !isQuotesSupported)
        throw new InvalidConfigException(
            "if quotes_required is true, quotes_supported must also be true for asset: " + assetId);
    }

    // Validate SEP-31 `receive.min_amount` and `receive.max_amount` fields
    if (receiveInfo.getMinAmount() < 0)
      throw new InvalidConfigException(
          "Invalid min_amount defined for asset "
              + assetId
              + ". sep31.receive.min_amount = "
              + receiveInfo.getMinAmount());
    if (receiveInfo.getMaxAmount() <= 0)
      throw new InvalidConfigException(
          "Invalid max_amount defined for asset "
              + assetId
              + ". sep31.receive.max_amount = "
              + receiveInfo.getMaxAmount());
  }

  private static void validateSep38(AssetService assetService, Sep38Info sep38Info, String assetId)
      throws InvalidConfigException {
    // Validate exchangeable_assets
    for (String exchangeableAsset : sep38Info.getExchangeableAssets()) {
      if (assetService.getAssetById(exchangeableAsset) == null)
        throw new InvalidConfigException(
            String.format(
                "Invalid exchangeable asset %s defined for asset %s.", exchangeableAsset, assetId));
    }

    // Validate country codes
    for (String country : sep38Info.getCountryCodes()) {
      if (!isCountryCodeValid(country))
        throw new InvalidConfigException(
            String.format("Invalid country code %s defined for asset %s.", country, assetId));
    }

    // Validate methods
    for (DeliveryMethod method : sep38Info.getBuyDeliveryMethods()) {
      if (StringHelper.isEmpty(method.getName()))
        throw new InvalidConfigException(
            String.format("Empty buy delivery method name defined for asset %s.", assetId));
      if (StringHelper.isEmpty(method.getDescription()))
        throw new InvalidConfigException(
            String.format("Empty buy delivery method description defined for asset %s.", assetId));
    }
  }

  private static boolean isCountryCodeValid(String countryCode) {
    return countryCode != null && countryCode.length() == 2 && isoCountries.contains(countryCode);
  }

  private static void validateDepositWithdrawInfo(DepositWithdrawInfo dwInfo, String assetId)
      throws InvalidConfigException {

    if (isWithdrawEnabled(dwInfo)) {
      // Check for missing SEP-6 withdrawal types
      if (isEmpty(dwInfo.getWithdraw().getMethods())) {
        throw new InvalidConfigException("Withdraw types not defined for asset " + assetId);
      }

      // Check for duplicate SEP-6 withdrawal types
      Set<String> existingWithdrawTypes = new HashSet<>();
      for (String type : dwInfo.getWithdraw().getMethods()) {
        if (!existingWithdrawTypes.add(type)) {
          throw new InvalidConfigException(
              "Duplicate withdraw types defined for asset " + assetId + ". Type = " + type);
        }
      }

      if (dwInfo.getWithdraw().getMinAmount() < 0) {
        throw new InvalidConfigException(
            "Invalid min_amount defined for asset "
                + assetId
                + ". withdraw.min_amount = "
                + dwInfo.getWithdraw().getMinAmount());
      }

      if (dwInfo.getWithdraw().getMaxAmount() <= 0) {
        throw new InvalidConfigException(
            "Invalid max_amount defined for asset "
                + assetId
                + ". deposit.max_amount = "
                + dwInfo.getWithdraw().getMaxAmount());
      }
    }

    // Validate deposit fields
    if (isDepositEnabled(dwInfo)) {
      // Check for missing SEP-6 deposit types
      if (isEmpty(dwInfo.getDeposit().getMethods())) {
        throw new InvalidConfigException("Deposit types not defined for asset " + assetId);
      }

      // Check for duplicate SEP-6 deposit types
      Set<String> existingDepositTypes = new HashSet<>();
      for (String type : dwInfo.getDeposit().getMethods()) {
        if (!existingDepositTypes.add(type)) {
          throw new InvalidConfigException(
              "Duplicate deposit types defined for asset " + assetId + ". Type = " + type);
        }
      }

      if (dwInfo.getDeposit().getMinAmount() < 0) {
        throw new InvalidConfigException(
            "Invalid min_amount defined for asset "
                + assetId
                + ". deposit.min_amount = "
                + dwInfo.getDeposit().getMinAmount());
      }

      if (dwInfo.getDeposit().getMaxAmount() <= 0) {
        throw new InvalidConfigException(
            "Invalid max_amount defined for asset "
                + assetId
                + ". deposit.max_amount = "
                + dwInfo.getDeposit().getMaxAmount());
      }
    }
  }
}
