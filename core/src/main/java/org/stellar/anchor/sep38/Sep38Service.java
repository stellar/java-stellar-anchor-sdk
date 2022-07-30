package org.stellar.anchor.sep38;

import static org.stellar.anchor.api.sep.sep38.Sep38Context.SEP31;
import static org.stellar.anchor.api.sep.sep38.Sep38Context.SEP6;
import static org.stellar.anchor.util.Log.debug;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.formatAmount;
import static org.stellar.anchor.util.SepHelper.validateAmount;
import static org.stellar.anchor.util.SepHelper.validateAmountLimit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.stellar.anchor.api.callback.GetRateRequest;
import org.stellar.anchor.api.callback.GetRateResponse;
import org.stellar.anchor.api.callback.RateIntegration;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.NotFoundException;
import org.stellar.anchor.api.exception.ServerErrorException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep38.*;
import org.stellar.anchor.api.shared.StellarId;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.event.EventPublishService;
import org.stellar.anchor.event.models.QuoteEvent;
import org.stellar.anchor.util.Log;

public class Sep38Service {
  final Sep38Config sep38Config;
  final AssetService assetService;
  final RateIntegration rateIntegration;
  final Sep38QuoteStore sep38QuoteStore;
  final EventPublishService eventService;
  final InfoResponse infoResponse;
  final Map<String, InfoResponse.Asset> assetMap;

  public Sep38Service(
      Sep38Config sep38Config,
      AssetService assetService,
      RateIntegration rateIntegration,
      Sep38QuoteStore sep38QuoteStore,
      EventPublishService eventService) {
    debug("sep38Config:", sep38Config);
    this.sep38Config = sep38Config;
    this.assetService = assetService;
    this.rateIntegration = rateIntegration;
    this.sep38QuoteStore = sep38QuoteStore;
    this.eventService = eventService;
    this.infoResponse = new InfoResponse(this.assetService.listAllAssets());
    assetMap = new HashMap<>();
    this.infoResponse.getAssets().forEach(asset -> assetMap.put(asset.getAsset(), asset));
    Log.info("Sep38Service initialized.");
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
            .type(GetRateRequest.Type.INDICATIVE_PRICES)
            .sellAsset(sellAssetName)
            .sellAmount(sellAmount)
            .countryCode(countryCode)
            .sellDeliveryMethod(sellDeliveryMethod)
            .buyDeliveryMethod(buyDeliveryMethod);
    GetPricesResponse response = new GetPricesResponse();
    for (String buyAssetName : sellAsset.getExchangeableAssetNames()) {
      InfoResponse.Asset buyAsset = this.assetMap.get(buyAssetName);
      if (buyAsset == null || !buyAsset.supportsBuyDeliveryMethod(buyDeliveryMethod)) {
        continue;
      }

      GetRateRequest request = builder.buyAsset(buyAssetName).build();
      GetRateResponse rateResponse = this.rateIntegration.getRate(request);
      GetRateResponse.Rate rate = rateResponse.getRate();
      response.addAsset(buyAssetName, buyAsset.getDecimals(), rate.getPrice());
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

  public GetPriceResponse getPrice(Sep38GetPriceRequest getPriceRequest) throws AnchorException {
    String sellAssetName = getPriceRequest.getSellAssetName();
    String sellAmount = getPriceRequest.getSellAmount();
    String sellDeliveryMethod = getPriceRequest.getSellDeliveryMethod();
    String buyAssetName = getPriceRequest.getBuyAssetName();
    String buyAmount = getPriceRequest.getBuyAmount();
    String buyDeliveryMethod = getPriceRequest.getBuyDeliveryMethod();
    String countryCode = getPriceRequest.getCountryCode();
    Sep38Context context = getPriceRequest.getContext();

    if (this.rateIntegration == null) {
      Log.error("rateIntegration should not be null!");
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

    // context
    if (context == null || !List.of(SEP6, SEP31).contains(context)) {
      throw new BadRequestException("Unsupported context. Should be one of [sep6, sep31].");
    }

    // Check SEP31 send limits
    if (sellAsset == null) {
      throw new BadRequestException("Unsupported sell asset.");
    }
    String[] assetCode = sellAsset.getAsset().split(":");
    AssetInfo asset = assetService.getAsset(assetCode[1]);
    Long sendMinLimit = asset.getSend().getMinAmount();
    Long sendMaxLimit = asset.getSend().getMaxAmount();

    // SEP31: when sell_amount is specified
    if (context == SEP31 && sellAmount != null) {
      validateAmountLimit("sell_", sellAmount, sendMinLimit, sendMaxLimit);
    }

    GetRateRequest request =
        GetRateRequest.builder()
            .type(GetRateRequest.Type.INDICATIVE_PRICE)
            .context(context)
            .sellAsset(sellAssetName)
            .sellAmount(sellAmount)
            .sellDeliveryMethod(sellDeliveryMethod)
            .buyAsset(buyAssetName)
            .buyAmount(buyAmount)
            .buyDeliveryMethod(buyDeliveryMethod)
            .countryCode(countryCode)
            .build();
    GetRateResponse rateResponse = this.rateIntegration.getRate(request);
    GetRateResponse.Rate rate = rateResponse.getRate();

    // SEP31: when buy_amount is specified (sell amount found from rate integration)
    if (context == SEP31 && buyAmount != null) {
      validateAmountLimit("sell_", rate.getSellAmount(), sendMinLimit, sendMaxLimit);
    }

    return GetPriceResponse.builder()
        .price(rate.getPrice())
        .totalPrice(rate.getTotalPrice())
        .fee(rate.getFee())
        .sellAmount(rate.getSellAmount())
        .buyAmount(rate.getBuyAmount())
        .build();
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

    // context
    Sep38Context context = request.getContext();
    if (context == null || !List.of(SEP6, SEP31).contains(context)) {
      throw new BadRequestException("Unsupported context. Should be one of [sep6, sep31].");
    }

    // Check SEP31 send limits
    if (sellAsset == null) {
      throw new BadRequestException("Unsupported sell asset.");
    }
    String[] assetCode = sellAsset.getAsset().split(":");
    AssetInfo asset = assetService.getAsset(assetCode[1]);
    Long sendMinLimit = asset.getSend().getMinAmount();
    Long sendMaxLimit = asset.getSend().getMaxAmount();

    // SEP31: when sell_amount is specified
    if (context == SEP31 && request.getSellAmount() != null) {
      validateAmountLimit("sell_", request.getSellAmount(), sendMinLimit, sendMaxLimit);
    }

    GetRateRequest getRateRequest =
        GetRateRequest.builder()
            .type(GetRateRequest.Type.FIRM)
            .context(request.getContext())
            .sellAsset(request.getSellAssetName())
            .sellAmount(request.getSellAmount())
            .sellDeliveryMethod(request.getSellDeliveryMethod())
            .buyAsset(request.getBuyAssetName())
            .buyAmount(request.getBuyAmount())
            .buyDeliveryMethod(request.getBuyDeliveryMethod())
            .countryCode(request.getCountryCode())
            .expireAfter(request.getExpireAfter())
            .clientId(account)
            .build();
    GetRateResponse.Rate rate = this.rateIntegration.getRate(getRateRequest).getRate();

    Sep38QuoteResponse.Sep38QuoteResponseBuilder builder =
        Sep38QuoteResponse.builder()
            .id(rate.getId())
            .expiresAt(rate.getExpiresAt())
            .price(rate.getPrice())
            .totalPrice(rate.getTotalPrice())
            .sellAsset(request.getSellAssetName())
            .buyAsset(request.getBuyAssetName())
            .fee(rate.getFee());

    // Calculate amounts: sellAmount = buyAmount * totalPrice
    BigDecimal bTotalPrice = decimal(rate.getTotalPrice());
    BigDecimal bSellAmount, bBuyAmount;
    if (request.getSellAmount() != null) {
      bSellAmount = decimal(request.getSellAmount());
      bBuyAmount = bSellAmount.divide(bTotalPrice, buyAsset.getDecimals(), RoundingMode.HALF_UP);
    } else {
      bBuyAmount = decimal(request.getBuyAmount());
      bSellAmount = bBuyAmount.multiply(bTotalPrice);
    }
    String sellAmount = formatAmount(bSellAmount, sellAsset.getDecimals());
    String buyAmount = formatAmount(bBuyAmount, buyAsset.getDecimals());
    builder = builder.sellAmount(sellAmount).buyAmount(buyAmount);

    // SEP31: when buy_amount is specified (sell amount found from rate integration)
    if (context == SEP31 && buyAmount != null) {
      validateAmountLimit("sell_", rate.getSellAmount(), sendMinLimit, sendMaxLimit);
    }

    // save firm quote in the local database
    Sep38Quote newQuote =
        new Sep38QuoteBuilder(this.sep38QuoteStore)
            .id(rate.getId())
            .expiresAt(rate.getExpiresAt())
            .price(rate.getPrice())
            .totalPrice(rate.getTotalPrice())
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
            .fee(rate.getFee())
            .build();

    this.sep38QuoteStore.save(newQuote);

    QuoteEvent event =
        QuoteEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .type(QuoteEvent.Type.QUOTE_CREATED)
            .id(newQuote.getId())
            .sellAsset(newQuote.getSellAsset())
            .sellAmount(newQuote.getSellAmount())
            .buyAsset(newQuote.getBuyAsset())
            .buyAmount(newQuote.getBuyAmount())
            .expiresAt(newQuote.getExpiresAt())
            .price(newQuote.getPrice())
            .totalPrice(newQuote.getTotalPrice())
            .creator(
                StellarId.builder()
                    .account(newQuote.getCreatorAccountId())
                    .build()) // TODO where to get StellarId.id?
            .transactionId(newQuote.getTransactionId())
            .createdAt(newQuote.getCreatedAt())
            .fee(rate.getFee())
            .build();

    eventService.publish(event);

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
        .totalPrice(quote.getTotalPrice())
        .price(quote.getPrice())
        .sellAsset(quote.getSellAsset())
        .sellAmount(quote.getSellAmount())
        .buyAsset(quote.getBuyAsset())
        .buyAmount(quote.getBuyAmount())
        .fee(quote.getFee())
        .build();
  }
}
