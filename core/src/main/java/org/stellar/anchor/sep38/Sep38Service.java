package org.stellar.anchor.sep38;

import static org.stellar.anchor.util.SepHelper.validateAmount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.dto.sep38.*;
import org.stellar.anchor.exception.*;
import org.stellar.anchor.integration.rate.GetRateRequest;
import org.stellar.anchor.integration.rate.GetRateResponse;
import org.stellar.anchor.integration.rate.RateIntegration;
import org.stellar.anchor.model.Sep38Quote;
import org.stellar.anchor.model.Sep38QuoteBuilder;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.util.Log;

public class Sep38Service {
  final Sep38Config sep38Config;
  final AssetService assetService;
  final RateIntegration rateIntegration;
  final Sep38QuoteStore sep38QuoteStore;
  final InfoResponse infoResponse;
  final Map<String, InfoResponse.Asset> assetMap;

  public Sep38Service(
      Sep38Config sep38Config,
      AssetService assetService,
      RateIntegration rateIntegration,
      Sep38QuoteStore sep38QuoteStore) {
    this.sep38Config = sep38Config;
    this.assetService = assetService;
    this.rateIntegration = rateIntegration;
    this.sep38QuoteStore = sep38QuoteStore;
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
            .type(GetRateRequest.Type.INDICATIVE)
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
            .type(GetRateRequest.Type.INDICATIVE)
            .sellAsset(sellAssetName)
            .sellAmount(sellAmount)
            .sellDeliveryMethod(sellDeliveryMethod)
            .buyAsset(buyAssetName)
            .buyAmount(buyAmount)
            .buyDeliveryMethod(buyDeliveryMethod)
            .countryCode(countryCode)
            .build();
    GetRateResponse rateResponse = this.rateIntegration.getRate(request);

    GetPriceResponse.GetPriceResponseBuilder builder =
        GetPriceResponse.builder().price(rateResponse.getRate().getPrice());

    // Calculate amounts: sellAmount = buyAmount*price or buyAmount = sellAmount/price
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

  public Sep38QuoteResponse postQuote(JwtToken token, Sep38PostQuoteRequest request)
      throws AnchorException {
    if (this.rateIntegration == null) {
      throw new ServerErrorException("internal server error");
    }

    if (this.sep38QuoteStore == null) {
      throw new ServerErrorException("internal server error");
    }

    // validate token
    if (token == null) {
      throw new BadRequestException("missing sep10 jwt token");
    }
    String account, memo = null, memoType = null;
    if (!Objects.toString(token.getMuxedAccount(), "").isEmpty()) {
      account = token.getMuxedAccount();
    } else if (!Objects.toString(token.getAccount(), "").isEmpty()) {
      account = token.getAccount();
      if (token.getAccountMemo() != null) {
        memo = token.getAccountMemo();
        memoType = "id";
      }
    } else {
      throw new BadRequestException("sep10 token is malformed");
    }

    validateAsset("sell_", request.getSellAssetName());
    validateAsset("buy_", request.getBuyAssetName());

    if ((request.getSellAmount() == null && request.getBuyAmount() == null)
        || (request.getSellAmount() != null && request.getBuyAmount() != null)) {
      throw new BadRequestException("Please provide either sell_amount or buy_amount");
    } else if (request.getSellAmount() != null) {
      validateAmount("sell_", request.getSellAmount());
    } else {
      validateAmount("buy_", request.getBuyAmount());
    }

    InfoResponse.Asset sellAsset = assetMap.get(request.getSellAssetName());
    InfoResponse.Asset buyAsset = assetMap.get(request.getBuyAssetName());

    // sellDeliveryMethod
    if (!Objects.toString(request.getSellDeliveryMethod(), "").isEmpty()) {
      if (!sellAsset.supportsSellDeliveryMethod(request.getSellDeliveryMethod())) {
        throw new BadRequestException("Unsupported sell delivery method");
      }
    }

    // buyDeliveryMethod
    if (!Objects.toString(request.getBuyDeliveryMethod(), "").isEmpty()) {
      if (!buyAsset.supportsBuyDeliveryMethod(request.getBuyDeliveryMethod())) {
        throw new BadRequestException("Unsupported buy delivery method");
      }
    }

    // countryCode
    if (!Objects.toString(request.getCountryCode(), "").isEmpty()) {
      List<String> sellCountryCodes = sellAsset.getCountryCodes();
      List<String> buyCountryCodes = buyAsset.getCountryCodes();
      boolean bothCountryCodesAreNull = sellCountryCodes == null && buyCountryCodes == null;
      boolean countryCodeIsSupportedForSell =
          sellCountryCodes != null && sellCountryCodes.contains(request.getCountryCode());
      boolean countryCodeIsSupportedForBuy =
          buyCountryCodes != null && buyCountryCodes.contains(request.getCountryCode());
      if (bothCountryCodesAreNull
          || !(countryCodeIsSupportedForSell || countryCodeIsSupportedForBuy)) {
        throw new BadRequestException("Unsupported country code");
      }
    }

    // expireAfter
    if (!Objects.toString(request.getExpireAfter(), "").isEmpty()) {
      try {
        Instant.parse(request.getExpireAfter());
      } catch (Exception ex) {
        throw new BadRequestException("expire_after is invalid");
      }
    }

    GetRateRequest getRateRequest =
        GetRateRequest.builder()
            .type(GetRateRequest.Type.FIRM)
            .sellAsset(request.getSellAssetName())
            .sellAmount(request.getSellAmount())
            .sellDeliveryMethod(request.getSellDeliveryMethod())
            .buyAsset(request.getBuyAssetName())
            .buyAmount(request.getBuyAmount())
            .buyDeliveryMethod(request.getBuyDeliveryMethod())
            .countryCode(request.getCountryCode())
            .expireAfter(request.getExpireAfter())
            .account(account)
            .memo(memo)
            .memoType(memoType)
            .build();
    GetRateResponse.Rate rate = this.rateIntegration.getRate(getRateRequest).getRate();

    Sep38QuoteResponse.Sep38QuoteResponseBuilder builder =
        Sep38QuoteResponse.builder()
            .id(rate.getId())
            .expiresAt(rate.getExpiresAt())
            .price(rate.getPrice())
            .sellAsset(request.getSellAssetName())
            .buyAsset(request.getBuyAssetName());

    // Calculate amounts: sellAmount = buyAmount*price or buyAmount = sellAmount/price
    BigDecimal bPrice = new BigDecimal(rate.getPrice());
    BigDecimal bSellAmount, bBuyAmount;
    if (request.getSellAmount() != null) {
      bSellAmount = new BigDecimal(request.getSellAmount());
      bBuyAmount = bSellAmount.divide(bPrice, buyAsset.getDecimals(), RoundingMode.HALF_UP);
    } else {
      bBuyAmount = new BigDecimal(request.getBuyAmount());
      bSellAmount = bBuyAmount.multiply(bPrice);
    }
    String sellAmount = formatAmount(bSellAmount, sellAsset.getDecimals());
    String buyAmount = formatAmount(bBuyAmount, buyAsset.getDecimals());
    builder = builder.sellAmount(sellAmount).buyAmount(buyAmount);

    // save firm quote in the local database
    Sep38Quote newQuote =
        new Sep38QuoteBuilder(this.sep38QuoteStore)
            .id(rate.getId())
            .expiresAt(rate.getExpiresAt())
            .price(rate.getPrice())
            .sellAsset(request.getSellAssetName())
            .sellAmount(sellAmount)
            .sellDeliveryMethod(request.getSellDeliveryMethod())
            .buyAsset(request.getBuyAssetName())
            .buyAmount(buyAmount)
            .buyDeliveryMethod(request.getBuyDeliveryMethod())
            .createdAt(Instant.now())
            .creatorAccountId(account)
            .creatorMemo(memo)
            .creatorMemoType(memoType)
            .build();
    this.sep38QuoteStore.save(newQuote);

    // TODO: create an event for `quote_created` using the event API
    return builder.build();
  }

  public Sep38QuoteResponse getQuote(JwtToken token, String quoteId) throws AnchorException {
    if (this.sep38QuoteStore == null) {
      throw new ServerErrorException("internal server error");
    }

    // validate token
    if (token == null) {
      throw new BadRequestException("missing sep10 jwt token");
    }
    String account, memo = null, memoType = null;
    if (!Objects.toString(token.getMuxedAccount(), "").isEmpty()) {
      account = token.getMuxedAccount();
    } else if (!Objects.toString(token.getAccount(), "").isEmpty()) {
      account = token.getAccount();
      if (token.getAccountMemo() != null) {
        memo = token.getAccountMemo();
        memoType = "id";
      }
    } else {
      throw new BadRequestException("sep10 token is malformed");
    }

    // empty quote id
    if (StringUtils.isEmpty(quoteId)) {
      throw new BadRequestException("quote id cannot be empty");
    }

    // validate consistency between quote and jwt token
    Sep38Quote quote = this.sep38QuoteStore.findByQuoteId(quoteId);
    if (quote == null
        || !StringUtils.equals(quote.getCreatorAccountId(), account)
        || !StringUtils.equals(memo, quote.getCreatorMemo())
        || !StringUtils.equals(memoType, quote.getCreatorMemoType())) {
      throw new NotFoundException("quote not found");
    }

    return Sep38QuoteResponse.builder()
        .id(quote.getId())
        .expiresAt(quote.getExpiresAt())
        .price(quote.getPrice())
        .sellAsset(quote.getSellAsset())
        .sellAmount(quote.getSellAmount())
        .buyAsset(quote.getBuyAsset())
        .buyAmount(quote.getBuyAmount())
        .build();
  }
}
