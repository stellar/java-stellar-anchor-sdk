package org.stellar.anchor.server.data;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import javax.persistence.*;
import lombok.Data;
import org.stellar.anchor.asset.AssetInfo;
import org.stellar.anchor.model.Sep31Transaction;
import org.stellar.anchor.util.GsonUtils;

@Data
@Entity
@Access(AccessType.FIELD)
public class JdbcSep31Transaction implements Sep31Transaction {
  static Gson gson = GsonUtils.getInstance();

  @Id String id;
  String status;

  @SerializedName("status_eta")
  Long statusEta;

  @SerializedName("amount_in")
  String amountIn;

  @SerializedName("amount_in_asset")
  String amountInAsset;

  @SerializedName("amount_out")
  String amountOut;

  @SerializedName("amount_out_asset")
  String amountOutAsset;

  @SerializedName("amount_fee")
  String amountFee;

  @SerializedName("amount_fee_asset")
  String amountFeeAsset;

  @SerializedName("stellar_account_id")
  String stellarAccountId;

  @SerializedName("stellar_memo_type")
  String stellarMemoType;

  @SerializedName("started_at")
  Instant startedAt;

  @SerializedName("completed_at")
  Instant completedAt;

  @SerializedName("stellar_transaction_id")
  String stellarTransactionId;

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  @SerializedName("required_info_message")
  String requiredInfoMessage;

  // Ignored by JPA
  @SerializedName("required_info_updates")
  @Transient
  AssetInfo.Sep31TxnFields requiredInfoUpdates;

  @Access(AccessType.PROPERTY)
  @Column(name = "requiredInfoUpdates")
  public String getRequiredInfoUpdatesJson() {
    return gson.toJson(this.requiredInfoUpdates);
  }

  public void setRequiredInfoUpdatesJson(String requiredInfoUpdatesJson) {
    if (requiredInfoUpdatesJson != null) {
      this.requiredInfoUpdates =
          gson.fromJson(requiredInfoUpdatesJson, AssetInfo.Sep31TxnFields.class);
    }
  }

  Boolean refunded;

  // Ignored by JPA
  @Transient Refunds refunds;

  @Access(AccessType.PROPERTY)
  @Column(name = "refunds")
  public String getRefundsJson() {
    return gson.toJson(this.refunds);
  }

  public void setRefundsJson(String refundsJson) {
    if (refundsJson != null) {
      this.refunds = gson.fromJson(refundsJson, Refunds.class);
    }
  }
}
