package org.stellar.anchor.sep12;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class PojoSep12Customer implements Sep12Customer {
  String id;

  String account;

  String memo;

  @SerializedName("memo_type")
  String memoType;
}
