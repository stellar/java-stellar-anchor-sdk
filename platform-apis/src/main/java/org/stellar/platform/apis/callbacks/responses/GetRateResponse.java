package org.stellar.platform.apis.callbacks.responses;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GetRateResponse {
  Rate rate;

  public GetRateResponse() {
    this.rate = new Rate();
  }

  public GetRateResponse(String price) {
    this.rate = new Rate();
    this.rate.price = price;
  }

  public GetRateResponse(String id, String price, LocalDateTime expiresAt) {
    this.rate = new Rate();
    this.rate.id = id;
    this.rate.price = price;
    this.rate.expiresAt = expiresAt;
  }

  @Data
  public static class Rate {
    String id;

    String price;

    @SerializedName("expires_at")
    LocalDateTime expiresAt;
  }
}
