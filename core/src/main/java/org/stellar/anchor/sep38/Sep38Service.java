package org.stellar.anchor.sep38;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.dto.sep38.GetPriceResponse;
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
    validateAsset("sell_", sellAssetName);
    validateAmount("sell_", sellAmount);

    InfoResponse.Asset sellAsset = assetMap.get(sellAssetName);

    // sellDeliveryMethod
    if (!Objects.toString(sellDeliveryMethod, "").isEmpty()) {
      if (!sellAsset.supportsSellDeliveryMethod(sellDeliveryMethod)) {
        throw new BadRequestException("Unsupported sell delivery method");
      }
    }

    // countryCode
    if (!Objects.toString(countryCode, "").isEmpty()) {
      if (!sellAsset.getCountryCodes().contains(countryCode)) {
        throw new BadRequestException("Unsupported country code");
      }
    }

    // Make requests to `GET {quoteIntegration}/rates`
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
      response.addAsset(buyAssetName, buyAsset.getDecimals(), rateResponse.getRate().getPrice());
    }

    return response;
  }

  public void validateAsset(String prefix, String assetName) throws AnchorException {
    // assetName
    if (Objects.toString(assetName, "").isEmpty()) {
      throw new BadRequestException(prefix + "asset cannot be empty");
    }

    InfoResponse.Asset asset = assetMap.get(assetName);
    if (asset == null) {
      throw new NotFoundException(prefix + "asset not found");
    }
  }

  public void validateAmount(String prefix, String amount) throws AnchorException {
    // assetName
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
  }

  public GetPriceResponse getPrice(
      String sellAssetName,
      String sellAmount,
      String sellDeliveryMethod,
      String buyAssetName,
      String buyAmount,
      String buyDeliveryMethod,
      String countryCode)
      throws AnchorException {
    if (this.rateIntegration == null) {
      throw new ServerErrorException("internal server error");
    }
    validateAsset("sell_", sellAssetName);
    validateAsset("buy_", buyAssetName);

    if ((sellAmount == null && buyAmount == null) || (sellAmount != null && buyAmount != null)) {
      throw new BadRequestException("Please provide either sell_amount or buy_amount");
    } else if (sellAmount != null) {
      validateAmount("sell_", sellAmount);
    } else {
      validateAmount("buy_", buyAmount);
    }

    InfoResponse.Asset sellAsset = assetMap.get(sellAssetName);
    InfoResponse.Asset buyAsset = assetMap.get(buyAssetName);

    // sellDeliveryMethod
    if (!Objects.toString(sellDeliveryMethod, "").isEmpty()) {
      if (!sellAsset.supportsSellDeliveryMethod(sellDeliveryMethod)) {
        throw new BadRequestException("Unsupported sell delivery method");
      }
    }

    // buyDeliveryMethod
    if (!Objects.toString(buyDeliveryMethod, "").isEmpty()) {
      if (!buyAsset.supportsBuyDeliveryMethod(buyDeliveryMethod)) {
        throw new BadRequestException("Unsupported buy delivery method");
      }
    }

    // countryCode
    if (!Objects.toString(countryCode, "").isEmpty()) {
      List<String> sellCountryCodes = sellAsset.getCountryCodes();
      List<String> buyCountryCodes = buyAsset.getCountryCodes();
      boolean bothCountryCodesAreNull = sellCountryCodes == null && buyCountryCodes == null;
      boolean countryCodeIsSupportedForSell =
          sellCountryCodes != null && sellCountryCodes.contains(countryCode);
      boolean countryCodeIsSupportedForBuy =
          buyCountryCodes != null && buyCountryCodes.contains(countryCode);
      if (bothCountryCodesAreNull
          || !(countryCodeIsSupportedForSell || countryCodeIsSupportedForBuy)) {
        throw new BadRequestException("Unsupported country code");
      }
    }

    GetRateRequest request =
        GetRateRequest.builder()
            .sellAsset(sellAssetName)
            .sellAmount(sellAmount)
            .sellDeliveryMethod(sellDeliveryMethod)
            .buyAsset(buyAssetName)
            .buyAmount(buyAmount)
            .buyDeliveryMethod(buyDeliveryMethod)
            .countryCode(countryCode)
            .build();
    GetRateResponse rateResponse = this.rateIntegration.getRate(request);

    // Calculate amounts: sellAmount = buyAmount*price or buyAmount = sellAmount/price
    GetPriceResponse.GetPriceResponseBuilder builder =
        GetPriceResponse.builder().price(rateResponse.getRate().getPrice());
    BigDecimal bPrice = new BigDecimal(rateResponse.getRate().getPrice());
    BigDecimal bSellAmount, bBuyAmount;
    if (sellAmount != null) {
      bSellAmount = new BigDecimal(sellAmount);
      bBuyAmount = bSellAmount.divide(bPrice, buyAsset.getDecimals(), RoundingMode.HALF_DOWN);
    } else {
      bBuyAmount = new BigDecimal(buyAmount);
      bSellAmount = bBuyAmount.multiply(bPrice);
    }
    builder =
        builder
            .sellAmount(formatAmount(bSellAmount, sellAsset.getDecimals()))
            .buyAmount(formatAmount(bBuyAmount, buyAsset.getDecimals()));

    return builder.build();
  }

  private String formatAmount(BigDecimal amount, Integer decimals) throws NumberFormatException {
    BigDecimal newAmount = amount.setScale(decimals, RoundingMode.HALF_DOWN);

    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(decimals);
    df.setMinimumFractionDigits(0);
    df.setGroupingUsed(false);

    return df.format(newAmount);
  }
}
