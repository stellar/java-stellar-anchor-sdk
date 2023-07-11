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
@Table(name = "transaction_pending_trust")
@TypeDef(name = "json", typeClass = JsonType.class)
@NoArgsConstructor
@AllArgsConstructor
public class JdbcTransactionPendingTrust {

  @Id String id;

  @SerializedName("created_at")
  @Column(name = "created_at")
  Instant createdAt;

  @SerializedName("asset")
  @Column(name = "asset")
  String asset;

  @SerializedName("account")
  @Column(name = "account")
  String account;

  @SerializedName("count")
  @Column(name = "count")
  int count;
}
