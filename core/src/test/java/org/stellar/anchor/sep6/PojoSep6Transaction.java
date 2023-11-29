package org.stellar.anchor.sep6;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.api.shared.InstructionField;
import org.stellar.anchor.api.shared.Refunds;
import org.stellar.anchor.api.shared.StellarTransaction;

@Data
public class PojoSep6Transaction implements Sep6Transaction {
  String id;
  List<StellarTransaction> stellarTransactions;
  String transactionId;
  String stellarTransactionId;
  String externalTransactionId;
  String status;
  Long statusEta;
  String moreInfoUrl;
  String kind;
  Instant startedAt;
  Instant completedAt;
  Instant updatedAt;
  Instant transferReceivedAt;
  String type;
  String requestAssetCode;
  String requestAssetIssuer;
  String amountIn;
  String amountInAsset;
  String amountOut;
  String amountOutAsset;
  String amountFee;
  String amountFeeAsset;
  String amountExpected;
  String sep10Account;
  String sep10AccountMemo;
  String withdrawAnchorAccount;
  String fromAccount;
  String toAccount;
  String memo;
  String memoType;
  String clientDomain;
  String clientName;
  String quoteId;
  String message;
  Refunds refunds;
  String refundMemo;
  String refundMemoType;
  String requiredInfoMessage;
  List<String> requiredInfoUpdates;
  String requiredCustomerInfoMessage;
  List<String> requiredCustomerInfoUpdates;
  Map<String, InstructionField> instructions;
}
