package org.stellar.anchor.sep31;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.Customers;
import org.stellar.anchor.api.shared.Refund;
import org.stellar.anchor.api.shared.StellarId;
import org.stellar.anchor.event.models.TransactionEvent;

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

  void setRequiredInfoMessage(String requiredInfoMessage);

  Map<String, String> getFields();

  void setFields(Map<String, String> fields);

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

  default Customers getCustomers() {
    return new Customers(
        StellarId.builder().id(getSenderId()).build(),
        StellarId.builder().id(getReceiverId()).build());
  }

  default Sep31GetTransactionResponse toSep31GetTransactionResponse() {
    Sep31GetTransactionResponse.Refunds refunds = null;
    if (getRefunds() != null) {
      List<Sep31GetTransactionResponse.Sep31RefundPayment> payments = null;
      if (getRefunds().getRefundPayments() != null) {
        for (RefundPayment refundPayment : getRefunds().getRefundPayments()) {
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
              .amountRefunded(getRefunds().getAmountRefunded())
              .amountFee(getRefunds().getAmountFee())
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

  default org.stellar.anchor.api.platform.GetTransactionResponse
      toPlatformApiGetTransactionResponse() {
    Refund refunds = null;
    if (getRefunds() != null) {
      refunds = getRefunds().toPlatformApiRefund(getAmountInAsset());
    }

    List<GetTransactionResponse.StellarTransaction> stellarTransactions = null;
    if (!Objects.toString(getStellarTransactionId(), "").isEmpty()) {
      GetTransactionResponse.StellarTransaction stellarTxn =
          new GetTransactionResponse.StellarTransaction();
      stellarTxn.setId(getStellarTransactionId());
      stellarTransactions = List.of(stellarTxn);
    }

    return org.stellar.anchor.api.platform.GetTransactionResponse.builder()
        .id(getId())
        .sep(31)
        .kind(TransactionEvent.Kind.RECEIVE.getKind())
        .status(getStatus())
        .amountExpected(new Amount(getAmountExpected(), getAmountInAsset()))
        .amountIn(new Amount(getAmountIn(), getAmountInAsset()))
        .amountOut(new Amount(getAmountOut(), getAmountOutAsset()))
        .amountFee(new Amount(getAmountFee(), getAmountFeeAsset()))
        .quoteId(getQuoteId())
        .startedAt(getStartedAt())
        .updatedAt(getUpdatedAt())
        .completedAt(getCompletedAt())
        .transferReceivedAt(getTransferReceivedAt())
        .message(getRequiredInfoMessage()) // Assuming these are meant to be the same.
        .refunds(refunds)
        .stellarTransactions(stellarTransactions)
        .externalTransactionId(getExternalTransactionId())
        // TODO .custodialTransactionId(txn.get)
        .customers(getCustomers())
        .creator(getCreator())
        .build();
  }
}
