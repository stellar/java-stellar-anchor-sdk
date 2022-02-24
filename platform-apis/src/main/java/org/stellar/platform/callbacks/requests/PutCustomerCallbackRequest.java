package org.stellar.platform.callbacks.requests;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class PutCustomerCallbackRequest {
  String id;
  String account;
  String memo;

  @SerializedName("memo_type")
  String memo_type;

  String url;
}
