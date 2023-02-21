package org.stellar.anchor.platform.data;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.stellar.anchor.api.shared.StellarTransaction;
import org.stellar.anchor.util.GsonUtils;

@Getter
@Setter
@MappedSuperclass
public abstract class JdbcSepTransaction {
  @Transient static Gson gson = GsonUtils.getInstance();

  String status;

  @SerializedName("updated_at")
  Instant updatedAt;

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

  @SerializedName("started_at")
  Instant startedAt;

  @SerializedName("completed_at")
  Instant completedAt;

  @SerializedName("transfer_received_at")
  Instant transferReceivedAt;

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  @Column(columnDefinition = "json")
  @Type(type = "json")
  List<StellarTransaction> stellarTransactions;

  public String getStellarTransactionId() {
    if (stellarTransactions != null && stellarTransactions.size() > 0) {
      return stellarTransactions.get(0).getId();
    }
    return null;
  }

  public abstract String getProtocol();
}
