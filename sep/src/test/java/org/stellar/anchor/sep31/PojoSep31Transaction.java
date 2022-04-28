package org.stellar.anchor.sep31;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.event.models.StellarTransaction;
import org.stellar.anchor.model.Sep31Transaction;

@Data
public class PojoSep31Transaction implements Sep31Transaction {
  String id;
  String status;
  Long statusEta;
  String amountIn;
  String amountInAsset;
  String amountOut;
  String amountOutAsset;
  String amountFee;
  String amountFeeAsset;
  String stellarAccountId;
  String stellarMemo;
  String stellarMemoType;
  Instant startedAt;
  Instant completedAt;
  String stellarTransactionId;
  String externalTransactionId;
  String requiredInfoMessage;
  String quoteId;
  String clientDomain;
  AssetInfo.Sep31TxnFieldSpecs requiredInfoUpdates;
  Map<String, String> fields;
  Boolean refunded;
  Refunds refunds;
  Instant updatedAt;
  Instant transferReceivedAt;
  String message;
  String amountExpected;
  Set<StellarTransaction> stellarTransactions = new java.util.LinkedHashSet<>();
}
