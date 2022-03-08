package org.stellar.platform.apis.api.requests;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GetTransactionsRequest {
  Integer sep;
  String order;

  @SerializedName("order_by")
  String orderBy;

  String cursor;
  LocalDateTime after;
  LocalDateTime before;
  String status;
}
