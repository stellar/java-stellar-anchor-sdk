package org.stellar.anchor.integration.rate;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import lombok.Data;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

@Data
public class GetRateResponse {
  @NonNull String price;

  @SerializedName("expires_at")
  @Nullable
  Date expiresAt;
}
