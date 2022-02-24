package org.stellar.platform.apis.api.requests;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GetQuotesRequest {
  String order;

  @SerializedName("order_by")
  String orderBy;

  String cursor;
  LocalDateTime after;
  LocalDateTime before;
}
