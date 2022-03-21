package org.stellar.anchor.integration.rate;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Data;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

@Data
public class GetRateResponse {
  Rate rate;

  public GetRateResponse(@NonNull String price) {
    this.rate = new Rate();
    this.rate.price = price;
  }

  public GetRateResponse(
      @NonNull String id, @NonNull String price, @NonNull LocalDateTime expiresAt) {
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
    LocalDateTime expiresAt;
  }
}
