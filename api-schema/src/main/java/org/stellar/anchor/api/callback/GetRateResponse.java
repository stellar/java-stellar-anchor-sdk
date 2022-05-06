package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import org.stellar.anchor.api.sep.sep38.PriceDetail;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

@Data
public class GetRateResponse {
  Rate rate;

  public GetRateResponse(@NonNull String price) {
    this.rate = new Rate();
    this.rate.price = price;
  }

  public GetRateResponse(@NonNull String id, @NonNull String price, @NonNull Instant expiresAt) {
    this.rate = new Rate();
    this.rate.id = id;
    this.rate.price = price;
    this.rate.expiresAt = expiresAt;
  }

  @Data
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
