package org.stellar.anchor.platform.data;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.event.models.StellarId;
import org.stellar.anchor.reference.model.StellarIdConverter;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.util.GsonUtils;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@Table(name = "sep31_transaction")
public class JdbcSep31Transaction implements Sep31Transaction, SepTransaction {
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

  @SerializedName("stellar_memo")
  String stellarMemo;

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

  @SerializedName("quote_id")
  String quoteId;

  @SerializedName("client_domain")
  String clientDomain;

  @SerializedName("sender_id")
  String senderId;

  @SerializedName("receiver_id")
  String receiverId;

  @Convert(converter = StellarIdConverter.class)
  @Column(length = 2047)
  StellarId creator;

  // Ignored by JPA and Gson
  @SerializedName("required_info_updates")
  @Transient
  AssetInfo.Sep31TxnFieldSpecs requiredInfoUpdates;

  // Ignored by JPA and Gson
  @SerializedName("fields")
  @Transient
  Map<String, String> fields;

  @Access(AccessType.PROPERTY)
  @Column(name = "fields")
  public String getFieldsJson() {
    return gson.toJson(this.fields);
  }

  public void setFieldsJson(String fieldsJson) {
    if (fieldsJson != null) {
      this.fields = gson.fromJson(fieldsJson, new TypeToken<Map<String, String>>() {}.getType());
    }
  }

  @Access(AccessType.PROPERTY)
  @Column(name = "requiredInfoUpdates")
  public String getRequiredInfoUpdatesJson() {
    return gson.toJson(this.requiredInfoUpdates);
  }

  public void setRequiredInfoUpdatesJson(String requiredInfoUpdatesJson) {
    if (requiredInfoUpdatesJson != null) {
      this.requiredInfoUpdates =
          gson.fromJson(requiredInfoUpdatesJson, AssetInfo.Sep31TxnFieldSpecs.class);
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

  Instant updatedAt;
  Instant transferReceivedAt;
  String message;
  String amountExpected;

  @OneToMany(fetch = FetchType.EAGER, mappedBy = "sep31Transaction")
  Set<StellarTransaction> stellarTransactions = new java.util.LinkedHashSet<>();
}
