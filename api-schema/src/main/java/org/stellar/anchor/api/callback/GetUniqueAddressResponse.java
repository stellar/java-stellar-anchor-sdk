package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * The response body of the GET /unique_address endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Callbacks%20API.yml">Callback
 *     API</a>
 */
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
