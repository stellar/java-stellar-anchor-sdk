package org.stellar.anchor.reference.service;

import static java.math.RoundingMode.HALF_DOWN;
import static org.stellar.anchor.api.callback.GetRateRequest.Type.*;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.formatAmount;
import static org.stellar.anchor.util.SepHelper.validateAmount;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import kotlin.Pair;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.callback.GetRateRequest;
import org.stellar.anchor.api.callback.GetRateResponse;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.NotFoundException;
import org.stellar.anchor.api.exception.UnprocessableEntityException;
import org.stellar.anchor.api.sep.sep38.RateFee;
import org.stellar.anchor.api.sep.sep38.RateFeeDetail;
import org.stellar.anchor.reference.model.Quote;
import org.stellar.anchor.reference.repo.QuoteRepo;
import org.stellar.anchor.util.DateUtil;

@Service
public class RateService {
  private final QuoteRepo quoteRepo;
  private static final int scale = 4;

  RateService(QuoteRepo quoteRepo) {
    this.quoteRepo = quoteRepo;
  }

  public GetRateResponse getRate(GetRateRequest request) throws AnchorException {
    if (request.getId() != null) {
      Quote quote = quoteRepo.findById(request.getId()).orElse(null);
      if (quote == null) {
        throw new NotFoundException("Quote not found.");
      }
      return quote.toGetRateResponse();
    }

    if (request.getType() == null) {
      throw new BadRequestException("type cannot be empty");
    }

    if (!List.of(INDICATIVE, FIRM).contains(request.getType())) {
      throw new BadRequestException("the provided type is not supported");
    }

    if (request.getSellAsset() == null) {
      throw new BadRequestException("sell_asset cannot be empty");
    }

    if (request.getBuyAsset() == null) {
      throw new BadRequestException("buy_asset cannot be empty");
    }

    String sellAmount = request.getSellAmount();
    String buyAmount = request.getBuyAmount();
    if ((sellAmount == null && buyAmount == null) || (sellAmount != null && buyAmount != null)) {
      throw new BadRequestException("Please provide either sell_amount or buy_amount");
    } else if (sellAmount != null) {
      validateAmount("sell_", sellAmount);
    } else {
      validateAmount("buy_", buyAmount);
    }

    // Calculate everything
    String price = ConversionPrice.getPrice(request.getSellAsset(), request.getBuyAsset());
    if (price == null) {
      throw new UnprocessableEntityException("the price for the given pair could not be found");
    }
    BigDecimal bPrice = decimal(price, scale);

    BigDecimal bSellAmount = null;
    BigDecimal bBuyAmount = null;
    if (sellAmount != null) {
      bSellAmount = decimal(sellAmount, scale);
    } else {
      bBuyAmount = decimal(buyAmount, scale);
    }

    RateFee fee = ConversionPrice.getFee(request.getSellAsset(), request.getBuyAsset());
    BigDecimal bFee = decimal(fee.getTotal());

    // sell_amount - fee = price * buy_amount     // when `fee` is in `sell_asset`
    if (bSellAmount != null) {
      // buy_amount = (sell_amount - fee) / price
      bBuyAmount = (bSellAmount.subtract(bFee)).divide(bPrice, HALF_DOWN);
      if (bBuyAmount.compareTo(BigDecimal.ZERO) < 0) {
        throw new BadRequestException("sell amount must be greater than " + fee.getTotal());
      }
      buyAmount = formatAmount(bBuyAmount, scale);
    } else {
      // sell_amount = (buy_amount * price) + fee
      bSellAmount = (bBuyAmount.setScale(10, HALF_DOWN).multiply(bPrice)).add(bFee);
      sellAmount = formatAmount(bSellAmount, scale);
    }
    // recalibrate price to guarantee the formula is true up to the required decimals
    bPrice = (bSellAmount.setScale(10, HALF_DOWN).subtract(bFee)).divide(bBuyAmount, HALF_DOWN);
    price = formatAmount(bPrice, 10);

    // total_price = sell_amount / buy_amount
    BigDecimal bTotalPrice = bSellAmount.divide(bBuyAmount, 10, HALF_DOWN);
    String totalPrice = formatAmount(bTotalPrice, 10);

    if (request.getType() == INDICATIVE) {
      return GetRateResponse.indicativePrice(price, sellAmount, buyAmount, fee);
    }

    Quote quote = createQuote(request, price, totalPrice, sellAmount, buyAmount, fee);
    return quote.toGetRateResponse();
  }

  private Quote createQuote(
      GetRateRequest request,
      String price,
      String totalPrice,
      String sellAmount,
      String buyAmount,
      RateFee fee) {
    Quote quote = Quote.of(request);
    quote.setPrice(price);
    quote.setTotalPrice(totalPrice);
    quote.setSellAmount(sellAmount);
    quote.setBuyAmount(buyAmount);
    quote.setFee(fee);

    // "calculate" expiresAt
    String strExpiresAfter = request.getExpireAfter();
    Instant expiresAfter;
    if (strExpiresAfter == null) {
      expiresAfter = Instant.now();
    } else {
      expiresAfter = DateUtil.fromISO8601UTC(strExpiresAfter);
    }

    ZonedDateTime expiresAt =
        ZonedDateTime.ofInstant(expiresAfter, ZoneId.of("UTC"))
            .plusDays(1)
            .withHour(12)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);
    quote.setExpiresAt(expiresAt.toInstant());

    quoteRepo.save(quote);
    return quote;
  }

  private static class ConversionPrice {
    private static final String fiatUSD = "iso4217:USD";
    private static final String stellarCircleUSDCtest =
        "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5";
    private static final String stellarUSDCtest =
        "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP";
    private static final String stellarUSDCprod =
        "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN";
    private static final String stellarJPYC =
        "stellar:JPYC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP";
    private static final Map<Pair<String, String>, String> hardcodedPrices =
        Map.ofEntries(
            Map.entry(new Pair<>(fiatUSD, stellarUSDCtest), "1.02"),
            Map.entry(new Pair<>(stellarUSDCtest, fiatUSD), "1.05"),
            Map.entry(new Pair<>(fiatUSD, stellarCircleUSDCtest), "1.02"),
            Map.entry(new Pair<>(stellarCircleUSDCtest, fiatUSD), "1.05"),
            Map.entry(new Pair<>(fiatUSD, stellarUSDCprod), "1.02"),
            Map.entry(new Pair<>(stellarUSDCprod, fiatUSD), "1.05"),
            Map.entry(new Pair<>(fiatUSD, stellarJPYC), "0.0083333"),
            Map.entry(new Pair<>(stellarJPYC, fiatUSD), "122"),
            Map.entry(new Pair<>(stellarUSDCtest, stellarJPYC), "0.0084"),
            Map.entry(new Pair<>(stellarJPYC, stellarUSDCtest), "120"),
            Map.entry(new Pair<>(stellarCircleUSDCtest, stellarJPYC), "0.0084"),
            Map.entry(new Pair<>(stellarJPYC, stellarCircleUSDCtest), "120"),
            Map.entry(new Pair<>(stellarUSDCprod, stellarJPYC), "0.0084"),
            Map.entry(new Pair<>(stellarJPYC, stellarUSDCprod), "120"));

    /*
    getPrice returns the price without fees
     */
    public static String getPrice(String sellAsset, String buyAsset) {
      return hardcodedPrices.get(new Pair<>(sellAsset, buyAsset));
    }

    public static RateFee getFee(String sellAsset, String buyAsset) {
      RateFee rateFee = new RateFee("0", sellAsset);
      if (getPrice(sellAsset, buyAsset) == null) {
        return rateFee;
      }

      RateFeeDetail sellAssetFeeDetail =
          new RateFeeDetail("Sell fee", "Fee related to selling the asset.", "1.00");
      rateFee.addFeeDetail(sellAssetFeeDetail);
      return rateFee;
    }
  }
}
