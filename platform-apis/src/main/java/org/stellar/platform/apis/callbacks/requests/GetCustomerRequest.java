package org.stellar.platform.apis.callbacks.requests;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class GetCustomerRequest {
  String id;
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  String type;
  String lang;
}
