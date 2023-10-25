package org.stellar.anchor.sep6;

import static org.stellar.anchor.util.SepHelper.amountEquals;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep38.RateFee;
import org.stellar.anchor.sep38.Sep38Quote;
import org.stellar.anchor.sep38.Sep38QuoteStore;

/** Calculates the amounts for an exchange request. */
@RequiredArgsConstructor
public class ExchangeAmountsCalculator {
  @NonNull private final Sep38QuoteStore sep38QuoteStore;

  /**
   * Calculates the amounts from a saved quote.
   *
   * @param quoteId The quote ID
   * @param sellAsset The asset the user is selling
   * @param sellAmount The amount the user is selling
   * @return The amounts
   * @throws AnchorException if the quote is invalid
   */
  public Amounts calculateFromQuote(String quoteId, AssetInfo sellAsset, String sellAmount)
      throws AnchorException {
    Sep38Quote quote = sep38QuoteStore.findByQuoteId(quoteId);
    if (quote == null) {
      throw new BadRequestException("Quote not found");
    }
    if (!amountEquals(sellAmount, quote.getSellAmount())) {
      throw new BadRequestException(
          String.format(
              "amount(%s) does not match quote sell amount(%s)",
              sellAmount, quote.getSellAmount()));
    }
    if (!sellAsset.getSep38AssetName().equals(quote.getSellAsset())) {
      throw new BadRequestException(
          String.format(
              "source asset(%s) does not match quote sell asset(%s)",
              sellAsset.getSep38AssetName(), quote.getSellAsset()));
    }
    RateFee fee = quote.getFee();
    if (fee == null) {
      throw new SepValidationException("Quote is missing the 'fee' field");
    }

    return Amounts.builder()
        .amountIn(quote.getSellAmount())
        .amountInAsset(quote.getSellAsset())
        .amountOut(quote.getBuyAmount())
        .amountOutAsset(quote.getBuyAsset())
        .amountFee(fee.getTotal())
        .amountFeeAsset(fee.getAsset())
        .build();
  }

  /** Amounts calculated for an exchange request. */
  @Builder
  @Data
  public static class Amounts {
    String amountIn;
    String amountInAsset;
    String amountOut;
    String amountOutAsset;
    String amountFee;
    String amountFeeAsset;
  }
}
