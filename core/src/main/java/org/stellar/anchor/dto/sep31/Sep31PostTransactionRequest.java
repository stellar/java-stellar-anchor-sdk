package org.stellar.anchor.dto.sep31;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.stellar.anchor.asset.AssetInfo;

@Data
public class Sep31PostTransactionRequest {
  String amount;

  @SerializedName("asset_code")
  String assetCode;

  @SerializedName("asset_issuer")
  String assetIssuer;

  @SerializedName("destination_asset")
  String destinationAsset;

  @SerializedName("quote_id")
  String quoteId;

  @SerializedName("sender_id")
  String senderId;

  @SerializedName("receiver_id")
  String receiverId;

  AssetInfo.Sep31TxnFields fields;
  String lang;
}
