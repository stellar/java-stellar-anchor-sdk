package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.stellar.anchor.api.sep.sep38.PriceDetail;
import reactor.util.annotation.Nullable;

@Data
@NoArgsConstructor
public class GetRateResponse {
  Rate rate;

  public static GetRateResponse indicative(String price, List<PriceDetail> priceDetails) {
    GetRateResponse getRateResponse = new GetRateResponse();
    getRateResponse.setRate(Rate.builder().price(price).priceDetails(priceDetails).build());
    return getRateResponse;
  }

  public static GetRateResponse firm(
      String id, String price, Instant expiresAt, List<PriceDetail> priceDetails) {
    GetRateResponse getRateResponse = new GetRateResponse();
    getRateResponse.setRate(
        Rate.builder().id(id).price(price).expiresAt(expiresAt).priceDetails(priceDetails).build());
    return getRateResponse;
  }

  @Data
  @Builder
  public static class Rate {
    @Nullable String id;

    String price;

    @SerializedName("expires_at")
    @Nullable
    Instant expiresAt;

    @SerializedName("price_details")
    List<PriceDetail> priceDetails;
  }
}
