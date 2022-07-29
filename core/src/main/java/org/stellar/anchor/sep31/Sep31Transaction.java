package org.stellar.anchor.sep31;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse;
import org.stellar.anchor.api.shared.Customers;
import org.stellar.anchor.api.shared.StellarId;

public interface Sep31Transaction {
  String getId();

  void setId(String id);

  String getStatus();

  void setStatus(String status);

  Long getStatusEta();

  void setStatusEta(Long statusEta);

  String getAmountExpected();

  void setAmountExpected(String amountExpected);

  String getAmountIn();

  void setAmountIn(String amountIn);

  String getAmountInAsset();

  void setAmountInAsset(String amountInAsset);

  String getAmountOut();

  void setAmountOut(String amountOut);

  String getAmountOutAsset();

  void setAmountOutAsset(String amountOutAsset);

  String getAmountFee();

  void setAmountFee(String amountFee);

  String getAmountFeeAsset();

  void setAmountFeeAsset(String amountFeeAsset);

  String getStellarAccountId();

  void setStellarAccountId(String stellarAccountId);

  String getStellarMemo();

  void setStellarMemo(String stellarMemo);

  String getStellarMemoType();

  void setStellarMemoType(String stellarMemoType);

  Instant getStartedAt();

  void setStartedAt(Instant startedAt);

  Instant getUpdatedAt();

  void setUpdatedAt(Instant updatedAt);

  Instant getTransferReceivedAt();

  void setTransferReceivedAt(Instant transferReceivedAt);

  Instant getCompletedAt();

  void setCompletedAt(Instant completedAt);

  String getStellarTransactionId();

  void setStellarTransactionId(String stellarTransactionId);

  String getExternalTransactionId();

  void setExternalTransactionId(String externalTransactionId);

  Boolean getRefunded();

  void setRefunded(Boolean refunded);

  Refunds getRefunds();

  void setRefunds(Refunds refunds);

  String getRequiredInfoMessage();

  Map<String, String> getFields();

  void setFields(Map<String, String> fields);

  void setRequiredInfoMessage(String requiredInfoMessage);

  AssetInfo.Sep31TxnFieldSpecs getRequiredInfoUpdates();

  void setRequiredInfoUpdates(AssetInfo.Sep31TxnFieldSpecs requiredInfoUpdates);

  String getQuoteId();

  void setQuoteId(String quoteId);

  String getClientDomain();

  void setClientDomain(String clientDomain);

  void setSenderId(String sourceId);

  String getSenderId();

  void setReceiverId(String destinationId);

  String getReceiverId();

  StellarId getCreator();

  void setCreator(StellarId creator);

  interface Refunds {

    String getAmountRefunded();

    void setAmountRefunded(String amountRefunded);

    String getAmountFee();

    void setAmountFee(String amountFee);

    List<RefundPayment> getRefundPayments();

    void setRefundPayments(List<RefundPayment> refundPayments);
  }

  interface RefundPayment {
    String getId();

    void setId(String id);

    String getAmount();

    void setAmount(String amount);

    String getFee();

    void setFee(String fee);
  }

  default Customers getCustomers() {
    return new Customers(
        StellarId.builder().id(getSenderId()).build(),
        StellarId.builder().id(getReceiverId()).build());
  }

  default Sep31GetTransactionResponse toSep31GetTransactionResponse() {
    Sep31GetTransactionResponse.Refunds refunds = null;
    if (this.getRefunds() != null) {
      List<Sep31GetTransactionResponse.Sep31RefundPayment> payments = null;
      if (this.getRefunds().getRefundPayments() != null) {
        for (Sep31Transaction.RefundPayment refundPayment : this.getRefunds().getRefundPayments()) {
          if (payments == null) {
            payments = new ArrayList<>();
          }

          payments.add(
              Sep31GetTransactionResponse.Sep31RefundPayment.builder()
                  .id(refundPayment.getId())
                  .amount(refundPayment.getAmount())
                  .fee(refundPayment.getFee())
                  .build());
        }
      }

      refunds =
          Sep31GetTransactionResponse.Refunds.builder()
              .amountRefunded(this.getRefunds().getAmountRefunded())
              .amountFee(this.getRefunds().getAmountFee())
              .payments(payments)
              .build();
    }

    return Sep31GetTransactionResponse.builder()
        .transaction(
            Sep31GetTransactionResponse.TransactionResponse.builder()
                .id(getId())
                .status(getStatus())
                .statusEta(getStatusEta())
                .amountIn(getAmountIn())
                .amountInAsset(getAmountInAsset())
                .amountOut(getAmountOut())
                .amountOutAsset(getAmountOutAsset())
                .amountFee(getAmountFee())
                .amountFeeAsset(getAmountFeeAsset())
                .stellarAccountId(getStellarAccountId())
                .stellarMemo(getStellarMemo())
                .stellarMemoType(getStellarMemoType())
                .startedAt(getStartedAt())
                .completedAt(getCompletedAt())
                .stellarTransactionId(getStellarTransactionId())
                .externalTransactionId(getExternalTransactionId())
                .refunded(getRefunded())
                .refunds(refunds)
                .requiredInfoMessage(getRequiredInfoMessage())
                .requiredInfoUpdates(getRequiredInfoUpdates())
                .build())
        .build();
  }
}
