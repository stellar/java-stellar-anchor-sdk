package org.stellar.anchor.sep6;

import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.formatAmount;
import static org.stellar.anchor.util.SepHelper.amountEquals;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.stellar.anchor.api.callback.FeeIntegration;
import org.stellar.anchor.api.callback.GetFeeRequest;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep38.RateFee;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.config.ClientsConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.sep38.Sep38Quote;
import org.stellar.anchor.sep38.Sep38QuoteStore;

/** Calculates the amounts for an exchange request. */
@RequiredArgsConstructor
public class ExchangeAmountsCalculator {
  @NonNull private final Sep10Config sep10Config;
  @NonNull private final ClientsConfig clientsConfig;
  @NonNull private final FeeIntegration feeIntegration;
  @NonNull private final Sep38QuoteStore sep38QuoteStore;
  @NonNull private final AssetService assetService;

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
    if (!sellAsset.getCode().equals(quote.getSellAsset())) {
      throw new BadRequestException(
          String.format(
              "source asset(%s) does not match quote sell asset(%s)",
              sellAsset.getCode(), quote.getSellAsset()));
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

  /**
   * Calculates the amounts for an exchange request by calling the Fee integration.
   *
   * @param buyAsset The asset the user is buying
   * @param sellAsset The asset the user is selling
   * @param amount The amount the user is selling
   * @param customerId The customer ID
   * @param sep10Jwt The SEP-10 JWT used to authenticate the request
   * @return The amounts
   * @throws AnchorException if the fee integration fails
   */
  public Amounts calculate(
      AssetInfo buyAsset, AssetInfo sellAsset, String amount, String customerId, Sep10Jwt sep10Jwt)
      throws AnchorException {
    String clientId = getClientId(sep10Jwt);
    Amount fee =
        feeIntegration
            .getFee(
                GetFeeRequest.builder()
                    .sendAmount(amount)
                    .sendAsset(sellAsset.getSep38AssetName())
                    .receiveAsset(buyAsset.getSep38AssetName())
                    .receiveAmount(null)
                    .senderId(customerId)
                    .receiverId(customerId)
                    .clientId(clientId)
                    .build())
            .getFee();

    AssetInfo feeAsset = assetService.getAssetByName(fee.getAsset());

    BigDecimal requestedAmount = decimal(amount, sellAsset.getSignificantDecimals());
    BigDecimal feeAmount = decimal(fee.getAmount(), feeAsset.getSignificantDecimals());

    BigDecimal amountOut = requestedAmount.subtract(feeAmount);

    return Amounts.builder()
        .amountIn(formatAmount(requestedAmount, buyAsset.getSignificantDecimals()))
        .amountInAsset(sellAsset.getSep38AssetName())
        .amountOut(formatAmount(amountOut, sellAsset.getSignificantDecimals()))
        .amountOutAsset(buyAsset.getSep38AssetName())
        .amountFee(formatAmount(feeAmount, feeAsset.getSignificantDecimals()))
        .amountFeeAsset(feeAsset.getSep38AssetName())
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

  private String getClientId(Sep10Jwt token) throws BadRequestException {
    ClientsConfig.ClientConfig clientByDomain =
        clientsConfig.getClientConfigByDomain(token.getClientDomain());
    ClientsConfig.ClientConfig clientByAccount =
        clientsConfig.getClientConfigBySigningKey(token.getAccount());
    ClientsConfig.ClientConfig client = clientByDomain != null ? clientByDomain : clientByAccount;

    if (!sep10Config.isClientAttributionRequired()) {
      return client != null ? client.getName() : null;
    }
    if (sep10Config.isClientAttributionRequired() && client == null) {
      throw new BadRequestException("Client not found");
    }

    if (sep10Config.getAllowedClientDomains().isEmpty()
        && sep10Config.getAllowedClientNames().isEmpty()) {
      return client.getName();
    }

    if (token.getClientDomain() != null) {
      if (sep10Config.getAllowedClientDomains().contains(client.getDomain())
          || sep10Config.getAllowedClientDomains().isEmpty()) {
        if (sep10Config.getAllowedClientNames().contains(client.getName())
            || sep10Config.getAllowedClientNames().isEmpty()) {
          return client.getName();
        }
        throw new BadRequestException("Client name not allowed");
      }
      throw new BadRequestException("Client domain not allowed");
    }

    if (sep10Config.getAllowedClientNames().contains(client.getName())
        || sep10Config.getAllowedClientNames().isEmpty()) {
      return client.getName();
    }
    throw new BadRequestException("Client name not allowed");
  }
}
