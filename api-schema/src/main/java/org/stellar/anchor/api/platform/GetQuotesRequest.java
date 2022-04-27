package org.stellar.anchor.api.platform;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Data;

@Data
public class GetQuotesRequest {
  String order;

  @SerializedName("order_by")
  String orderBy;

  String cursor;
  Instant after;
  Instant before;
}
