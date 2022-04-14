package org.stellar.anchor.dto.sep12;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Sep12DeleteCustomerRequest {
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;
}
