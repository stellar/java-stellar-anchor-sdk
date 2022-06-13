package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

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
