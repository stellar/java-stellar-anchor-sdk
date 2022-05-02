package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetCustomerRequest {
  String id;
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  String type;
  String lang;
}
