package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;
import org.stellar.anchor.api.sep.sep38.RateFee;
import org.stellar.anchor.reference.model.RateFeeConverter;
import org.stellar.anchor.sep38.Sep38Quote;

@Data
@Entity
@Table(name = "exchange_quote")
public class JdbcSep38Quote implements Sep38Quote {
  @Id String id;

  @SerializedName("expires_at")
  Instant expiresAt;

  String price;

  @SerializedName("total_price")
  String totalPrice;

  @SerializedName("sell_asset")
  String sellAsset;

  @SerializedName("sell_amount")
  String sellAmount;

  @SerializedName("sell_delivery_method")
  String sellDeliveryMethod;

  @SerializedName("buy_asset")
  String buyAsset;

  @SerializedName("buy_amount")
  String buyAmount;

  @SerializedName("buy_delivery_method")
  String buyDeliveryMethod;

  @SerializedName("created_at")
  Instant createdAt;

  @SerializedName("creator_account_id")
  String creatorAccountId;

  @SerializedName("creator_memo")
  String creatorMemo;

  @SerializedName("creator_memo_type")
  String creatorMemoType;

  @SerializedName("transaction_id")
  String transactionId;

  @Convert(converter = RateFeeConverter.class)
  @Column(length = 1023)
  RateFee fee;
}
