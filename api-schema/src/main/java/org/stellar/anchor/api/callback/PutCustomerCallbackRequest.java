package org.stellar.anchor.api.callback;

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
