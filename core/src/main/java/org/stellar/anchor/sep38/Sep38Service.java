package org.stellar.anchor.sep38;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
  final InfoResponse infoResponse;
  final Map<String, InfoResponse.Asset> assetMap;

  public Sep38Service(
      Sep38Config sep38Config, AssetService assetService, RateIntegration rateIntegration) {
    this.sep38Config = sep38Config;
    this.assetService = assetService;
    this.rateIntegration = rateIntegration;
    this.infoResponse = new InfoResponse(this.assetService.listAllAssets());
    assetMap = new HashMap<>();
    this.infoResponse.getAssets().forEach(asset -> assetMap.put(asset.getAsset(), asset));
    Log.info("Initializing sep38 service.");
  }

  public InfoResponse getInfo() {
    return this.infoResponse;
  }

  public GetPricesResponse getPrices(
      String sellAssetName,
      String sellAmount,
      String sellDeliveryMethod,
      String buyDeliveryMethod,
      String countryCode)
      throws AnchorException {
    if (this.rateIntegration == null) {
      throw new ServerErrorException("internal server error");
    }
    validateAssetWithMsgPrefix("sell_", sellAssetName, sellAmount, sellDeliveryMethod, null);

    InfoResponse.Asset sellAsset = assetMap.get(sellAssetName);

    // countryCode
    if (!Objects.toString(countryCode, "").isEmpty()) {
      if (!sellAsset.getCountryCodes().contains(countryCode)) {
        throw new BadRequestException("Unsupported country code");
      }
    }

    GetRateRequest.GetRateRequestBuilder builder =
        GetRateRequest.builder()
            .sellAsset(sellAssetName)
            .sellAmount(sellAmount)
            .countryCode(countryCode)
            .sellDeliveryMethod(sellDeliveryMethod)
            .buyDeliveryMethod(buyDeliveryMethod);
    GetPricesResponse response = new GetPricesResponse();
    for (String buyAssetName : sellAsset.getExchangeableAssetNames()) {
      InfoResponse.Asset buyAsset = this.assetMap.get(buyAssetName);
      if (!buyAsset.supportsBuyDeliveryMethod(buyDeliveryMethod)) {
        continue;
      }

      GetRateRequest request = builder.buyAsset(buyAssetName).build();
      GetRateResponse rateResponse = this.rateIntegration.getRate(request);
      response.addAsset(buyAssetName, rateResponse.getRate().getPrice());
    }

    return response;
  }

  public void validateAssetWithMsgPrefix(
      String prefix,
      String assetName,
      String amount,
      String sellDeliveryMethod,
      String buyDeliveryMethod)
      throws AnchorException {
    Log.infoF(
        "validateAssetWithMsgPrefix(): prefix={}, assetName={}, amount={}, sellDeliveryMethod={}, buyDeliveryMethod={}",
        prefix,
        assetName,
        amount,
        sellDeliveryMethod,
        buyDeliveryMethod);

    // assetName
    if (Objects.toString(assetName, "").isEmpty()) {
      throw new BadRequestException(prefix + "asset cannot be empty");
    }

    InfoResponse.Asset sellAsset = assetMap.get(assetName);
    if (sellAsset == null) {
      throw new NotFoundException(prefix + "asset not found");
    }

    // amount
    if (Objects.toString(amount, "").isEmpty()) {
      throw new BadRequestException(prefix + "amount cannot be empty");
    }

    BigDecimal sAmount;
    try {
      sAmount = new BigDecimal(amount);
    } catch (NumberFormatException e) {
      throw new BadRequestException(prefix + "amount is invalid", e);
    }
    if (sAmount.signum() < 1) {
      throw new BadRequestException(prefix + "amount should be positive");
    }

    // sellDeliveryMethod
    if (!Objects.toString(sellDeliveryMethod, "").isEmpty()) {
      if (!sellAsset.supportsSellDeliveryMethod(sellDeliveryMethod)) {
        throw new BadRequestException("Unsupported sell delivery method");
      }
    }

    // buyDeliveryMethod
    if (!Objects.toString(buyDeliveryMethod, "").isEmpty()) {
      if (!sellAsset.supportsBuyDeliveryMethod(buyDeliveryMethod)) {
        throw new BadRequestException("Unsupported buy delivery method");
      }
    }
  }
}
