package org.stellar.anchor.sep31;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.shared.StellarId;
import org.stellar.anchor.api.shared.StellarTransaction;

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
  List<StellarTransaction> stellarTransactions;
  String externalTransactionId;
  String requiredInfoMessage;
  String quoteId;
  String clientDomain;
  AssetInfo.Sep31TxnFieldSpecs requiredInfoUpdates;
  Map<String, String> fields;
  Boolean refunded;
  PojoSep31Refunds refunds;
  Instant updatedAt;
  Instant transferReceivedAt;
  String amountExpected;
  String receiverId;
  String senderId;
  StellarId creator;

  @Override
  public void setRefunds(Refunds refunds) {
    if (refunds == null) {
      this.refunds = null;
      return;
    }

    PojoSep31Refunds newRefunds = new PojoSep31Refunds();
    newRefunds.setAmountRefunded(refunds.getAmountRefunded());
    newRefunds.setAmountFee(refunds.getAmountFee());
    newRefunds.setRefundPayments(refunds.getRefundPayments());
    this.refunds = newRefunds;
  }
}
