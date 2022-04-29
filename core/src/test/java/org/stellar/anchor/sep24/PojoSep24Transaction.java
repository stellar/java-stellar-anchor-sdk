package org.stellar.anchor.sep24;

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
  String stellarAccount;
  String receivingAnchorAccount;
  String fromAccount;
  String toAccount;
  String memoType;
  String memo;
  String protocol;
  String domainClient;
  Boolean claimableBalanceSupported;
  String amountIn;
  String amountOut;
  String amountFee;
  String amountInAsset;
  String amountOutAsset;
  String amountFeeAsset;
  String accountMemo;
  String muxedAccount;
  Boolean refunded;
  PojoSep24Refunds refunds;
}
