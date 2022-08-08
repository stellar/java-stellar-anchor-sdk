package org.stellar.anchor.platform.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.stellar.anchor.sep24.Sep24RefundPayment;
import org.stellar.anchor.sep24.Sep24Transaction;

@Getter
@Setter
@Entity
@Table(name = "sep24_transaction")
public class JdbcSep24Transaction implements Sep24Transaction, SepTransaction {
  @Id
  @GeneratedValue
  @Column(name = "sep_transaction_id")
  UUID jdbcId;

  String id;

  String transactionId;

  String stellarTransactionId;

  String externalTransactionId;

  String status;

  String kind;

  Long startedAt;

  Long completedAt;

  String assetCode; // *

  String assetIssuer; // *

  String sep10Account; // *

  String sep10AccountMemo; // *

  String withdrawAnchorAccount;

  String fromAccount; // *

  String toAccount; // *

  String memoType;

  String memo;

  String clientDomain;

  Boolean claimableBalanceSupported;

  String amountIn;

  String amountOut;

  String amountFee;

  String amountInAsset;

  String amountOutAsset;

  String amountFeeAsset;

  String muxedAccount;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "transaction")
  List<JdbcSep24RefundPayment> refundPayments;

  @Override
  public List<? extends Sep24RefundPayment> getRefundPayments() {
    return refundPayments;
  }

  @Override
  public void setRefundPayments(List<? extends Sep24RefundPayment> payments) {
    refundPayments = new ArrayList<>(payments.size());
    payments.stream()
        .filter(p -> p instanceof JdbcSep24RefundPayment)
        .forEach(fp -> refundPayments.add((JdbcSep24RefundPayment) fp));
  }

  @Override
  public String getId() {
    return jdbcId.toString();
  }

  @Override
  public void setId(String id) {
    jdbcId = UUID.fromString(id);
  }
}
