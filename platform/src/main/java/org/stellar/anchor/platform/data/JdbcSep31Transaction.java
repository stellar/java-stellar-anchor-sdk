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
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.shared.StellarId;
import org.stellar.anchor.reference.model.StellarIdConverter;
import org.stellar.anchor.sep31.Sep31Refunds;
import org.stellar.anchor.sep31.Sep31Transaction;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@Table(name = "sep31_transaction")
@TypeDef(name = "json", typeClass = JsonType.class)
@NoArgsConstructor
public class JdbcSep31Transaction extends JdbcSepTransaction
    implements Sep31Transaction, SepTransaction {
  public String getProtocol() {
    return "31";
  }

  @Id String id;

  @SerializedName("status_eta")
  Long statusEta;

  @SerializedName("stellar_account_id")
  String stellarAccountId;

  @SerializedName("stellar_memo")
  String stellarMemo;

  @SerializedName("stellar_memo_type")
  String stellarMemoType;

  @SerializedName("quote_id")
  String quoteId;

  @SerializedName("client_domain")
  String clientDomain;

  @SerializedName("sender_id")
  String senderId;

  @SerializedName("receiver_id")
  String receiverId;

  @SerializedName("required_info_message")
  String requiredInfoMessage;

  @Convert(converter = StellarIdConverter.class)
  @Column(length = 2047)
  StellarId creator;

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

  // Ignored by JPA and Gson
  @SerializedName("required_info_updates")
  @Transient
  AssetInfo.Sep31TxnFieldSpecs requiredInfoUpdates;

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

  @Column(columnDefinition = "json")
  @Type(type = "json")
  JdbcSep31Refunds refunds;

  public Sep31Refunds getRefunds() {
    return refunds;
  }

  public void setRefunds(Sep31Refunds sep31Refunds) {
    if (sep31Refunds != null) {
      this.refunds = new JdbcSep31Refunds();
      BeanUtils.copyProperties(sep31Refunds, this.refunds);
    }
  }

  String amountExpected;
}
