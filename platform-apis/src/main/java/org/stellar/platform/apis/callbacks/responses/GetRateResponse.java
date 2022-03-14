package org.stellar.platform.apis.callbacks.responses;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GetRateResponse {
  Rate rate;

  @Data
  public static class Rate {
    String price;

    @SerializedName("expires_at")
    LocalDateTime expiresAt;
  }
}
