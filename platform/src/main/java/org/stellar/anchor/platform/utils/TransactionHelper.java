package org.stellar.anchor.platform.utils;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.*;

import javax.annotation.Nullable;
import lombok.SneakyThrows;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.api.platform.PlatformTransactionData;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.*;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSep31Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24RefundPayment;
import org.stellar.anchor.sep24.Sep24Refunds;
import org.stellar.anchor.sep31.Sep31Refunds;

public class TransactionHelper {
  @SneakyThrows
  public static GetTransactionResponse toGetTransactionResponse(
      JdbcSepTransaction txn, AssetService assetService) {
    switch (txn.getProtocol()) {
      case "24":
        return toGetTransactionResponse((JdbcSep24Transaction) txn, assetService);
      case "31":
        return toGetTransactionResponse((JdbcSep31Transaction) txn);
      default:
        throw new SepException(String.format("Unsupported protocol:%s", txn.getProtocol()));
    }
  }

  static GetTransactionResponse toGetTransactionResponse(JdbcSep31Transaction txn) {
    Refunds refunds = null;
    if (txn.getRefunds() != null) {
      refunds = toRefunds(txn.getRefunds(), txn.getAmountInAsset());
    }

    return GetTransactionResponse.builder()
        .id(txn.getId())
        .sep(PlatformTransactionData.Sep.SEP_31)
        .kind(RECEIVE)
        .status(SepTransactionStatus.from(txn.getStatus()))
        .amountExpected(new Amount(txn.getAmountExpected(), txn.getAmountInAsset()))
        .amountIn(new Amount(txn.getAmountIn(), txn.getAmountInAsset()))
        .amountOut(new Amount(txn.getAmountOut(), txn.getAmountOutAsset()))
        .amountFee(new Amount(txn.getAmountFee(), txn.getAmountFeeAsset()))
        .quoteId(txn.getQuoteId())
        .startedAt(txn.getStartedAt())
        .updatedAt(txn.getUpdatedAt())
        .completedAt(txn.getCompletedAt())
        .transferReceivedAt(txn.getTransferReceivedAt())
        .message(txn.getRequiredInfoMessage()) // Assuming these are meant to be the same.
        .refunds(refunds)
        .stellarTransactions(txn.getStellarTransactions())
        .externalTransactionId(txn.getExternalTransactionId())
        .customers(txn.getCustomers())
        .creator(txn.getCreator())
        .build();
  }

  static GetTransactionResponse toGetTransactionResponse(
      JdbcSep24Transaction txn, AssetService assetService) {
    Refunds refunds = null;
    if (txn.getRefunds() != null) {
      refunds = toRefunds(txn.getRefunds(), txn.getAmountInAsset());
    }

    String amountInAsset = makeAsset(txn.getAmountInAsset(), assetService, txn);
    String amountOutAsset = makeAsset(txn.getAmountOutAsset(), assetService, txn);
    String amountFeeAsset = makeAsset(txn.getAmountFeeAsset(), assetService, txn);
    String amountExpectedAsset = makeAsset(null, assetService, txn);
    String sourceAccount = txn.getFromAccount();

    return GetTransactionResponse.builder()
        .id(txn.getId())
        .sep(PlatformTransactionData.Sep.SEP_24)
        .kind(PlatformTransactionData.Kind.from(txn.getKind()))
        .status(SepTransactionStatus.from(txn.getStatus()))
        .amountExpected(new Amount(txn.getAmountExpected(), amountExpectedAsset))
        .amountIn(Amount.create(txn.getAmountIn(), amountInAsset))
        .amountOut(Amount.create(txn.getAmountOut(), amountOutAsset))
        .amountFee(Amount.create(txn.getAmountFee(), amountFeeAsset))
        .startedAt(txn.getStartedAt())
        .updatedAt(txn.getUpdatedAt())
        .completedAt(txn.getCompletedAt())
        .transferReceivedAt(txn.getTransferReceivedAt())
        .message(txn.getMessage())
        .refunds(refunds)
        .stellarTransactions(txn.getStellarTransactions())
        // constructor is used because AMOUNT can be null, when ASSET is always non-null
        .sourceAccount(sourceAccount)
        .destinationAccount(txn.getToAccount())
        .externalTransactionId(txn.getExternalTransactionId())
        .memo(txn.getMemo())
        .memoType(txn.getMemoType())
        .build();
  }

  private static String makeAsset(
      @Nullable String dbAsset, AssetService service, JdbcSep24Transaction txn) {
    if (dbAsset != null) {
      return dbAsset;
    }

    AssetInfo info = service.getAsset(txn.getRequestAssetCode(), txn.getRequestAssetIssuer());

    // Already validated in the interactive flow
    return info.getAssetName();
  }

  static RefundPayment toRefundPayment(Sep24RefundPayment refundPayment, String assetName) {
    return RefundPayment.builder()
        .id(refundPayment.getId())
        .idType(RefundPayment.IdType.STELLAR)
        .amount(new Amount(refundPayment.getAmount(), assetName))
        .fee(new Amount(refundPayment.getFee(), assetName))
        .requestedAt(null)
        .refundedAt(null)
        .build();
  }

  static RefundPayment toRefundPayment(
      org.stellar.anchor.sep31.RefundPayment refundPayment, String assetName) {
    return RefundPayment.builder()
        .id(refundPayment.getId())
        .idType(RefundPayment.IdType.STELLAR)
        .amount(new Amount(refundPayment.getAmount(), assetName))
        .fee(new Amount(refundPayment.getFee(), assetName))
        .requestedAt(null)
        .refundedAt(null)
        .build();
  }

  static Refunds toRefunds(Sep24Refunds refunds, String assetName) {
    // build payments
    RefundPayment[] payments =
        refunds.getRefundPayments().stream()
            .map(refundPayment -> toRefundPayment(refundPayment, assetName))
            .toArray(RefundPayment[]::new);

    return Refunds.builder()
        .amountRefunded(new Amount(refunds.getAmountRefunded(), assetName))
        .amountFee(new Amount(refunds.getAmountFee(), assetName))
        .payments(payments)
        .build();
  }

  static Refunds toRefunds(Sep31Refunds refunds, String assetName) {
    // build payments
    RefundPayment[] payments =
        refunds.getRefundPayments().stream()
            .map(refundPayment -> toRefundPayment(refundPayment, assetName))
            .toArray(RefundPayment[]::new);

    return Refunds.builder()
        .amountRefunded(new Amount(refunds.getAmountRefunded(), assetName))
        .amountFee(new Amount(refunds.getAmountFee(), assetName))
        .payments(payments)
        .build();
  }
}
