package org.stellar.anchor.sep12;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class PojoSep12CustomerId implements Sep12CustomerId {
  String id;

  String account;

  String memo;

  @SerializedName("memo_type")
  String memoType;
}
