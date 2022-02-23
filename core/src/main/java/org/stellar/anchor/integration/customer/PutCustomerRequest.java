package org.stellar.anchor.integration.customer;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class PutCustomerRequest extends org.stellar.anchor.dto.sep12.PutCustomerRequest {
  String id;
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  String type;
  Map<String, String> sep9Fields = new HashMap<>();
}
