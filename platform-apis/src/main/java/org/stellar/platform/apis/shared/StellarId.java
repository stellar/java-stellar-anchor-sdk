package org.stellar.platform.apis.shared;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class StellarId {
  String id;
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;
}
