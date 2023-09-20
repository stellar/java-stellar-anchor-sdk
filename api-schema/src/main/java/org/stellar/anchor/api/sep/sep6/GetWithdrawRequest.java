package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * The request body of the GET /withdraw endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0006.md#request-1">GET
 *     /withdraw</a>
 */
@Builder
@Data
public class GetWithdrawRequest {
  /** The asset code of the asset to withdraw. */
  @SerializedName("asset_code")
  @NonNull
  String assetCode;

  /** Type of withdrawal. */
  @NonNull String type;

  /** The amount to withdraw. */
  @NonNull String amount;

  /** The ISO 3166-1 alpha-3 code of the user's current address. */
  @SerializedName("country_code")
  String countryCode;

  /** The memo the anchor must use when sending refund payments back to the user. */
  @SerializedName("refund_memo")
  String refundMemo;

  /** The type of the refund_memo. */
  @SerializedName("refund_memo_type")
  String refundMemoType;
}
