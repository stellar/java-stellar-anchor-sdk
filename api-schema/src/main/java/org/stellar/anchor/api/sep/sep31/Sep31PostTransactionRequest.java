package org.stellar.anchor.api.sep.sep31;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Data;

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

  Sep31TxnFields fields;
  String lang;

  @Data
  @AllArgsConstructor
  public static class Sep31TxnFields {
    HashMap<String, String> transaction;
  }

  public String getAssetName(){
    if(assetIssuer != null){
      return "stellar:" + assetCode + ":" + assetIssuer;
    }
    return assetCode;
  }

}
