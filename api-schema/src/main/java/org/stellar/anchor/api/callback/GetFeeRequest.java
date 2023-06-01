package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * The request body of the GET /fee endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/ap/Callbacks%20API.yml">Callback
 *     API</a>
 */
@Data
@Builder
public class GetFeeRequest {
  @SerializedName("send_asset")
  String sendAsset;

  @SerializedName("receive_asset")
  String receiveAsset;

  @SerializedName("send_amount")
  String sendAmount;

  @SerializedName("receive_amount")
  String receiveAmount;

  @SerializedName("client_id")
  String clientId;

  @SerializedName("sender_id")
  String senderId;

  @SerializedName("receiver_id")
  String receiverId;
}
