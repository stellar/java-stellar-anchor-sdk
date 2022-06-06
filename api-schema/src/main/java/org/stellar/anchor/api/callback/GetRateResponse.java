package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.stellar.anchor.api.sep.sep38.RateFee;
import reactor.util.annotation.Nullable;

@Data
@NoArgsConstructor
public class GetRateResponse {
  public GetRateResponse(Rate rate) {
    this.rate = rate;
  }

  Rate rate;

  /**
   * Builds the response expected for the INDICATIVE_PRICES type
   *
   * @param price the price between sell asset and buy asset, where price = sell_amount /
   *     buy_amount. Fees are ignored in this response.
   * @param sellAmount the amount of sell_asset the anchor would expect to receive.
   * @param buyAmount the amount of buy_asset the anchor would trade for the sell_amount of
   *     sell_asset.
   * @return a GET /rate response with price, sell_amount and buy_amount.
   */
  public static GetRateResponse indicativePrices(
      String price, String sellAmount, String buyAmount) {
    Rate rate = Rate.builder().price(price).sellAmount(sellAmount).buyAmount(buyAmount).build();
    return new GetRateResponse(rate);
  }

  /**
   * Builds the response expected for the INDICATIVE_PRICE type.
   *
   * @param price the price between sell asset and buy asset, without including fees, where
   *     `sell_amount - fee = price * buy_amount` or `sell_amount = price * (buy_amount + fee)` must
   *     be true.
   * @param totalPrice the total price between sell_asset and buy_asset, where `total_price =
   *     sell_amount / buy_amount`. Fees are included in this price.
   * @param sellAmount the amount of sell_asset the anchor would expect to receive.
   * @param buyAmount the amount of buy_asset the anchor would trade for the sell_amount of
   *     sell_asset.
   * @param fee an object describing the fee used to calculate the conversion price.
   * @return a GET /rate response with price, total_price, sell_amount, buy_amount and fee.
   */
  public static GetRateResponse indicativePrice(
      String price, String totalPrice, String sellAmount, String buyAmount, RateFee fee) {
    Rate rate =
        Rate.builder()
            .price(price)
            .totalPrice(totalPrice)
            .sellAmount(sellAmount)
            .buyAmount(buyAmount)
            .fee(fee)
            .build();
    return new GetRateResponse(rate);
  }

  @Data
  @Builder
  public static class Rate {
    @Nullable String id;

    @SerializedName("total_price")
    @Nullable
    String totalPrice;

    String price;

    @SerializedName("sell_amount")
    @Nullable
    String sellAmount;

    @SerializedName("buy_amount")
    @Nullable
    String buyAmount;

    @SerializedName("expires_at")
    @Nullable
    Instant expiresAt;

    @Nullable RateFee fee;
  }
}
