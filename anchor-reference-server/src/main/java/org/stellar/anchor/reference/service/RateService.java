package org.stellar.anchor.reference.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import kotlin.Pair;
import org.springframework.stereotype.Service;
import org.stellar.anchor.exception.*;
import org.stellar.anchor.reference.model.Quote;
import org.stellar.anchor.reference.repo.QuoteRepo;
import org.stellar.platform.apis.callbacks.requests.GetRateRequest;
import org.stellar.platform.apis.callbacks.responses.GetRateResponse;

@Service
public class RateService {
  private final QuoteRepo quoteRepo;

  RateService(QuoteRepo quoteRepo) {
    this.quoteRepo = quoteRepo;
  }

  public GetRateResponse getRate(GetRateRequest request) throws AnchorException {
    if (request.getId() != null) {
      throw new ServerErrorException("getting quote by id is not implemented yet");
    }

    if (request.getType() == null) {
      throw new BadRequestException("type cannot be empty");
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

    String price = ConversionPrice.getPrice(request.getSellAsset(), request.getBuyAsset());
    if (price == null) {
      throw new UnprocessableEntityException("the price for the given pair could not be found");
    }

    if (request.getType().equals("indicative")) {
      return new GetRateResponse(price);
    } else if (request.getType().equals("firm")) {
      Quote newQuote = createQuote(request, price);
      return new GetRateResponse(newQuote.getId(), newQuote.getPrice(), newQuote.getExpiresAt());
    }
    throw new BadRequestException("type is not supported");
  }

  private Quote createQuote(GetRateRequest request, String price) {
    Quote quote = Quote.of(request, price);

    // "calculate" expiresAt
    Instant expiresAfter = request.getExpiresAfter();
    if (expiresAfter == null) {
      expiresAfter = Instant.now();
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
    private static final String stellarUSDCtest =
        "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5";
    private static final String stellarUSDCprod =
        "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN";
    private static final String stellarJPYC =
        "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5";
    private static final Map<Pair<String, String>, String> hardcodedPrices =
        Map.of(
            new Pair<>(fiatUSD, stellarUSDCtest), "1.02",
            new Pair<>(stellarUSDCtest, fiatUSD), "1.05",
            new Pair<>(fiatUSD, stellarUSDCprod), "1.02",
            new Pair<>(stellarUSDCprod, fiatUSD), "1.05",
            new Pair<>(fiatUSD, stellarJPYC), "0.0083333",
            new Pair<>(stellarJPYC, fiatUSD), "122",
            new Pair<>(stellarUSDCtest, stellarJPYC), "0.0084",
            new Pair<>(stellarJPYC, stellarUSDCtest), "120",
            new Pair<>(stellarUSDCprod, stellarJPYC), "0.0084",
            new Pair<>(stellarJPYC, stellarUSDCprod), "120");

    public static String getPrice(String sellAsset, String buyAsset) {
      return hardcodedPrices.get(new Pair<>(sellAsset, buyAsset));
    }
  }

  private void validateAmount(String prefix, String amount) throws AnchorException {
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
}
