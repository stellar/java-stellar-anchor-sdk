package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Entity
@Access(AccessType.FIELD)
@Table(name = "transaction_pending_trust")
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
}
