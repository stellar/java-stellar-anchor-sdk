package org.stellar.anchor.platform.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import javax.persistence.*;
import lombok.Data;
import org.stellar.anchor.platform.data.JdbcSep31Transaction;
import org.stellar.anchor.util.GsonUtils;

@Entity
@Table(name = "stellar_transaction")
@Data
public class StellarTransaction {
  private static Gson gson = GsonUtils.getInstance();

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "sep_31_transaction_id")
  private JdbcSep31Transaction sep31Transaction;

  String memo;

  @SerializedName("memo_type")
  String memoType;

  @SerializedName("created_at")
  Instant createdAt;

  String envelope;

  transient String paymentJson;

  @Access(AccessType.PROPERTY)
  @Column(name = "payment")
  public String getPaymentJson() {
    return gson.toJson(this.payment);
  }

  public void setPaymentJson(String requiredInfoUpdatesJson) {
    if (requiredInfoUpdatesJson != null) {
      this.payment = gson.fromJson(paymentJson, Payment.class);
    }
  }

  @Transient Payment payment;

  @Data
  class Payment {}
}
