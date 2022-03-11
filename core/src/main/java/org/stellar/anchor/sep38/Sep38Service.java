package org.stellar.anchor.sep38;

import java.util.List;
import java.util.Objects;
import org.stellar.anchor.asset.AssetInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.dto.sep38.GetPricesResponse;
import org.stellar.anchor.dto.sep38.InfoResponse;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.exception.BadRequestException;
import org.stellar.anchor.exception.NotFoundException;
import org.stellar.anchor.exception.ServerErrorException;
import org.stellar.anchor.integration.rate.GetRateRequest;
import org.stellar.anchor.integration.rate.GetRateResponse;
import org.stellar.anchor.integration.rate.RateIntegration;
import org.stellar.anchor.util.Log;

public class Sep38Service {
  final Sep38Config sep38Config;
  final AssetService assetService;
  final RateIntegration rateIntegration;

  public Sep38Service(
      Sep38Config sep38Config, AssetService assetService, RateIntegration rateIntegration) {
    this.sep38Config = sep38Config;
    this.assetService = assetService;
    this.rateIntegration = rateIntegration;
    Log.info("Initializing sep38 service.");
  }

  public InfoResponse getInfo() {
    List<AssetInfo> assets = this.assetService.listAllAssets();
    return new InfoResponse(assets);
  }

  public GetPricesResponse getPrices(
      String sellAssetName,
      String sellAmount,
      String countryCode,
      String sellDeliveryMethod,
      String buyDeliveryMethod)
      throws AnchorException {
    if (this.rateIntegration == null) {
      throw new ServerErrorException("internal server error");
    }
    validateGetPricesInput(
        sellAssetName, sellAmount, countryCode, sellDeliveryMethod, buyDeliveryMethod);
    InfoResponse.Asset sellAsset =
        getInfo().getAssets().stream()
            .filter(asset -> asset.getAsset().equals(sellAssetName))
            .findFirst()
            .orElse(null);
    assert sellAsset != null;

    GetRateRequest.GetRateRequestBuilder builder =
        GetRateRequest.builder()
            .sellAsset(sellAssetName)
            .sellAmount(sellAmount)
            .countryCode(countryCode)
            .sellDeliveryMethod(sellDeliveryMethod)
            .buyDeliveryMethod(buyDeliveryMethod);
    GetPricesResponse response = new GetPricesResponse();
    for (String buyAssetName : sellAsset.getExchangeableAssetNames()) {
      GetRateRequest request = builder.buyAsset(buyAssetName).build();
      GetRateResponse rateResponse = this.rateIntegration.getRate(request);
      response.addAsset(buyAssetName, rateResponse.getRate().getPrice());
    }

    return response;
  }

  public void validateGetPricesInput(
      String sellAssetName,
      String sellAmount,
      String countryCode,
      String sellDeliveryMethod,
      String buyDeliveryMethod)
      throws AnchorException {
    Log.infoF(
        "validateGetPricesInput(): sellAssetName={}, sellAmount={}, countryCode={}, sellDeliveryMethod={}, buyDeliveryMethod={}",
        sellAssetName,
        sellAmount,
        countryCode,
        sellDeliveryMethod,
        buyDeliveryMethod);

    if (Objects.toString(sellAssetName, "").isEmpty()) {
      throw new BadRequestException("sell_asset cannot be empty");
    }

    InfoResponse.Asset sellAsset =
        getInfo().getAssets().stream()
            .filter(asset -> asset.getAsset().equals(sellAssetName))
            .findFirst()
            .orElse(null);
    if (sellAsset == null) {
      throw new NotFoundException("sell_asset not found");
    }

    if (Objects.toString(sellAmount, "").isEmpty()) {
      throw new BadRequestException("sell_amount cannot be empty");
    }

    if (!Objects.toString(countryCode, "").isEmpty()) {
      if (!sellAsset.getCountryCodes().contains(countryCode)) {
        throw new BadRequestException("Unsupported country code");
      }
    }

    if (!Objects.toString(sellDeliveryMethod, "").isEmpty()) {
      if (sellAsset.getSellDeliveryMethods() == null) {
        throw new BadRequestException("Unsupported sell delivery method");
      }

      AssetInfo.Sep38Operation.DeliveryMethod deliveryMethod =
          sellAsset.getSellDeliveryMethods().stream()
              .filter(dMethod -> dMethod.getName().equals(sellDeliveryMethod))
              .findFirst()
              .orElse(null);
      if (deliveryMethod == null) {
        throw new BadRequestException("Unsupported sell delivery method");
      }
    }

    if (!Objects.toString(buyDeliveryMethod, "").isEmpty()) {
      if (sellAsset.getBuyDeliveryMethods() == null) {
        throw new BadRequestException("Unsupported buy delivery method");
      }

      AssetInfo.Sep38Operation.DeliveryMethod deliveryMethod =
          sellAsset.getBuyDeliveryMethods().stream()
              .filter(dMethod -> dMethod.getName().equals(buyDeliveryMethod))
              .findFirst()
              .orElse(null);
      if (deliveryMethod == null) {
        throw new BadRequestException("Unsupported buy delivery method");
      }
    }
  }
}
