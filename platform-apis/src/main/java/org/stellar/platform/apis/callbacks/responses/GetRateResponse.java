package org.stellar.platform.apis.callbacks.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
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
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Rate {
    String id;

    String price;

    @SerializedName("expires_at")
    @JsonProperty("expires_at")
    LocalDateTime expiresAt;
  }
}
