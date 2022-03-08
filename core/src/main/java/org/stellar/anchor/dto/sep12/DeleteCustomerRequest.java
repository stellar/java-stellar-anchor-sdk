package org.stellar.anchor.dto.sep12;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class DeleteCustomerRequest {
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;
}
