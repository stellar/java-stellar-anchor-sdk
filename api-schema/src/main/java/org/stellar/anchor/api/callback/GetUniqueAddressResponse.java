package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class GetUniqueAddressResponse {
  @SerializedName("unique_address")
  UniqueAddress uniqueAddress;

  @Data
  public static class UniqueAddress {
    @SerializedName("stellar_address")
    String stellarAddress;

    String memo;

    @SerializedName("memo_type")
    String memoType;
  }
}
