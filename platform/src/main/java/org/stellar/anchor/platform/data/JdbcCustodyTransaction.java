package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.time.Instant;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TypeDef;

@Getter
@Setter
@Builder
@Entity
@Access(AccessType.FIELD)
@Table(name = "custody_transaction")
@TypeDef(name = "json", typeClass = JsonType.class)
@NoArgsConstructor
@AllArgsConstructor
public class JdbcCustodyTransaction {

  @Id String id;

  @SerializedName("external_id")
  @Column(name = "external_id")
  String externalTxId;

  @SerializedName("status")
  @Column(name = "status")
  String status;

  @SerializedName("amount")
  @Column(name = "amount")
  String amount;

  @SerializedName("amount_asset")
  @Column(name = "amount_asset")
  String amountAsset;

  @SerializedName("started_at")
  @Column(name = "started_at")
  Instant createdAt;

  @SerializedName("updated_at")
  @Column(name = "updated_at")
  Instant updatedAt;

  @SerializedName("memo")
  @Column(name = "memo")
  String memo;

  @SerializedName("memo_type")
  @Column(name = "memo_type")
  String memoType;

  @SerializedName("protocol")
  @Column(name = "protocol")
  String protocol;

  @SerializedName("from_account")
  @Column(name = "from_account")
  String fromAccount;

  @SerializedName("to_account")
  @Column(name = "to_account")
  String toAccount;

  @SerializedName("kind")
  @Column(name = "kind")
  String kind;
}
