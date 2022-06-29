package org.stellar.anchor.sep24;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PojoSep24Transaction implements Sep24Transaction {
  String id;
  String documentType;
  String transactionId;
  String stellarTransactionId;
  String externalTransactionId;
  String status;
  String kind;
  Long startedAt;
  Long completedAt;
  String assetCode;
  String assetIssuer;
  String sep10Account;
  String sep10AccountMemo;
  String withdrawAnchorAccount;
  String fromAccount;
  String toAccount;
  String memoType;
  String memo;
  String protocol;
  String clientDomain;
  Boolean claimableBalanceSupported;
  String amountIn;
  String amountOut;
  String amountFee;
  String amountInAsset;
  String amountOutAsset;
  String amountFeeAsset;
  String muxedAccount;
  private List<PojoSep24RefundPayment> refundPayments;

  @Override
  public void setRefundPayments(List<? extends Sep24RefundPayment> payments) {
    refundPayments = new ArrayList<>(payments.size());
    payments.stream()
        .filter(p -> p instanceof PojoSep24RefundPayment)
        .forEach(fp -> refundPayments.add((PojoSep24RefundPayment) fp));
  }
}
