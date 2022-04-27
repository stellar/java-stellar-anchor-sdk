package org.stellar.anchor.api.shared;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class CustomerStatus {
  String id;

  @SerializedName("sep31_send")
  KYCStatus sep31Send;

  @SerializedName("sep31_receive")
  KYCStatus sep31Receive;

  @SerializedName("deposit")
  KYCStatus deposit;

  @SerializedName("withdraw")
  KYCStatus withdraw;
}

@Data
class KYCStatus {
  String type;
}
