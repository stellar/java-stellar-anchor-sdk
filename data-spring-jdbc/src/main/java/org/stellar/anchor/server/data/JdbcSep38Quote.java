package org.stellar.anchor.server.data;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;
import org.stellar.anchor.model.Sep38Quote;

@Data
@Entity
public class JdbcSep38Quote implements Sep38Quote {
  @Id String id;

  @SerializedName("expires_at")
  LocalDateTime expiresAt;

  String price;

  @SerializedName("sell_asset")
  String sellAsset;

  @SerializedName("sell_amount")
  String sellAmount;

  @SerializedName("buy_asset")
  String buyAsset;

  @SerializedName("buy_amount")
  String buyAmount;

  @SerializedName("created_at")
  LocalDateTime createdAt;

  @SerializedName("creator_account_id")
  String creatorAccountId;

  @SerializedName("creator_memo")
  String creatorMemo;

  @SerializedName("transaction_id")
  String transactionId;
}
