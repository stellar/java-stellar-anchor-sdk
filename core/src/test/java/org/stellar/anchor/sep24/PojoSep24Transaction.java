package org.stellar.anchor.sep24;

import java.time.Instant;
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
  Instant startedAt;
  Instant completedAt;
  String requestAssetCode;
  String requestAssetIssuer;
  String sep10Account;
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

  Boolean refunded;
  Sep24Refunds refunds;
  String requestedAmount;
}
