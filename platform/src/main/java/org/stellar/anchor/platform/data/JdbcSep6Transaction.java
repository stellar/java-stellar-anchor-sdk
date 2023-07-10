package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.util.Map;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.beans.BeanUtils;
import org.stellar.anchor.sep6.Sep6Refunds;
import org.stellar.anchor.sep6.Sep6Transaction;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@TypeDef(name = "json", typeClass = JsonType.class)
@NoArgsConstructor
public class JdbcSep6Transaction extends JdbcSepTransaction
    implements Sep6Transaction, SepTransaction {
  public String getProtocol() {
    return "6";
  }

  String kind;

  @SerializedName("transaction_id")
  @Column(name = "transaction_id")
  String transactionId;

  @SerializedName("request_asset_code")
  @Column(name = "request_asset_code")
  String requestAssetCode;

  @SerializedName("request_asset_issuer")
  @Column(name = "request_asset_issuer")
  String requestAssetIssuer;

  @SerializedName("amount_expected")
  @Column(name = "amount_expected")
  String amountExpected;

  @SerializedName("sep10_account")
  @Column(name = "sep10_account")
  String sep10Account;

  @SerializedName("sep10_account_memo")
  @Column(name = "sep10_account_memo")
  String sep10AccountMemo;

  @SerializedName("withdraw_anchor_account")
  @Column(name = "withdraw_anchor_account")
  String withdrawAnchorAccount;

  @SerializedName("from_account")
  @Column(name = "from_account")
  String fromAccount;

  @SerializedName("to_account")
  @Column(name = "to_account")
  String toAccount;

  @SerializedName("memo")
  @Column(name = "memo")
  String memo;

  @SerializedName("memo_type")
  @Column(name = "memo_type")
  String memoType;

  @SerializedName("claimable_balance_supported")
  @Column(name = "claimable_balance_supported")
  Boolean claimableBalanceSupported;

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

  @SerializedName("quote_id")
  @Column(name = "quote_id")
  String quoteId;

  @SerializedName("refunded")
  @Column(name = "refunded")
  Boolean refunded;

  @Column(columnDefinition = "json")
  @Type(type = "json")
  JdbcSep6Refunds refunds;

  @Override
  public Sep6Refunds getRefunds() {
    return refunds;
  }

  @Override
  public void setRefunds(Sep6Refunds refunds) {
    if (refunds != null) {
      this.refunds = new JdbcSep6Refunds();
      BeanUtils.copyProperties(refunds, this.refunds);
    }
  }

  @SerializedName("refund_memo")
  @Column(name = "refund_memo")
  String refundMemo;

  @SerializedName("refund_memo_type")
  @Column(name = "refund_memo_type")
  String refundMemoType;
}
