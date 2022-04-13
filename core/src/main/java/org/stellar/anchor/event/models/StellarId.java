package org.stellar.anchor.event.models;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class StellarId {
  String id;
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  public StellarId() {}
}
