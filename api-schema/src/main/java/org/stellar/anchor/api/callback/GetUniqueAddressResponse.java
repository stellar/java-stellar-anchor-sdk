package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetUniqueAddressResponse {
  @SerializedName("unique_address")
  UniqueAddress uniqueAddress;

  @Data
  @Builder
  public static class UniqueAddress {
    @SerializedName("stellar_address")
    String stellarAddress;

    String memo;

    @SerializedName("memo_type")
    String memoType;
  }
}
