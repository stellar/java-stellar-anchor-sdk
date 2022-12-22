package org.stellar.anchor.platform.service;

import static org.stellar.anchor.api.sep.SepTransactionStatus.*;
import static org.stellar.anchor.sep31.Sep31Helper.allAmountAvailable;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.equalsAsDecimals;

import io.micrometer.core.instrument.Metrics;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.api.platform.PatchTransactionRequest;
import org.stellar.anchor.api.platform.PatchTransactionsRequest;
import org.stellar.anchor.api.platform.PatchTransactionsResponse;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.RefundPayment;
import org.stellar.anchor.api.shared.Refunds;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSep31Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24RefundPayment;
import org.stellar.anchor.sep24.Sep24Refunds;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31Refunds;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep38.Sep38Quote;
import org.stellar.anchor.sep38.Sep38QuoteStore;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.SepHelper;
import org.stellar.anchor.util.StringHelper;

@Service
public class TransactionService {
  private final Sep38QuoteStore quoteStore;
  private final Sep31TransactionStore txn31Store;
  private final Sep24TransactionStore txn24Store;
  private final List<AssetInfo> assets;

  static boolean isStatusError(String status) {
    return List.of(PENDING_CUSTOMER_INFO_UPDATE.getName(), EXPIRED.getName(), ERROR.getName())
        .contains(status);
  }

  TransactionService(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Sep38QuoteStore quoteStore,
      AssetService assetService) {
    this.txn24Store = txn24Store;
    this.txn31Store = txn31Store;
    this.quoteStore = quoteStore;
    this.assets = assetService.listAllAssets();
  }

  /**
   * Fetch the transaction and convert the transaction to an object of GetTransactionResponse class.
   *
   * @param txnId the transaction ID
   * @return the result
   */
  public GetTransactionResponse getTransactionResponse(String txnId) throws AnchorException {
    if (Objects.toString(txnId, "").isEmpty()) {
      Log.info("Rejecting GET {platformApi}/transaction/:id because the id is empty.");
      throw new BadRequestException("transaction id cannot be empty");
    }
    JdbcSepTransaction txn = findTransaction(txnId);
    if (txn != null) {
      return toGetTransactionResponse(txn);
    } else {
      throw new NotFoundException(String.format("transaction (id=%s) is not found", txnId));
    }
  }

  /**
   * Fetch the transaction.
   *
   * @param txnId the transaction ID
   * @return an object of JdbcSepTransaction
   */
  public JdbcSepTransaction findTransaction(String txnId) throws AnchorException {
    JdbcSep31Transaction txn31 = (JdbcSep31Transaction) txn31Store.findByTransactionId(txnId);
    if (txn31 != null) {
      return txn31;
    }

    return (JdbcSep24Transaction) txn24Store.findByTransactionId(txnId);
  }

  /**
   * Patch transactions.
   *
   * @param request the request
   * @return the response
   */
  public PatchTransactionsResponse patchTransactions(PatchTransactionsRequest request)
      throws AnchorException {
    List<PatchTransactionRequest> patchRequests = request.getRecords();

    //    PatchTransactionsResponse patchTransactionsResponse = new PatchTransactionsResponse();
    List<GetTransactionResponse> txnResponses = new LinkedList<>();
    for (PatchTransactionRequest patchRequest : patchRequests) {
      txnResponses.add(patchTransaction(patchRequest));
    }
    return new PatchTransactionsResponse(txnResponses);
  }

  private GetTransactionResponse patchTransaction(PatchTransactionRequest patch)
      throws AnchorException {
    JdbcSepTransaction txn = findTransaction(patch.getId());
    if (txn == null)
      throw new BadRequestException(String.format("transaction(id=%s) not found", patch.getId()));
    switch (txn.getProtocol()) {
      case "24":
        patchSep24Transaction((JdbcSep24Transaction) txn, patch);
        break;
      case "31":
        patchSep31Transaction((JdbcSep31Transaction) txn, patch);
        break;
    }
    return toGetTransactionResponse(txn);
  }

  private void patchSep24Transaction(JdbcSep24Transaction txn, PatchTransactionRequest patch)
      throws AnchorException {
    String txnOriginalStatus = txn.getStatus();

    updateSepTransaction(patch, txn);
    txn24Store.save(txn);

    // Add them to the to-be-updated lists.
    if (!txnOriginalStatus.equals(txn.getStatus()))
      Metrics.counter(AnchorMetrics.SEP24_TRANSACTION.toString(), "status", txn.getStatus())
          .increment();
  }

  private void patchSep31Transaction(JdbcSep31Transaction txn, PatchTransactionRequest patch)
      throws AnchorException {
    String txnOriginalStatus = txn.getStatus();
    // validate and update the transaction.
    updateSepTransaction(patch, txn);
    // Add them to the to-be-updated lists.
    if (!txnOriginalStatus.equals(txn.getStatus()))
      Metrics.counter(AnchorMetrics.SEP31_TRANSACTION.toString(), "status", txn.getStatus())
          .increment();

    txn31Store.save(txn);
  }

  void updateSepTransaction(PatchTransactionRequest patch, JdbcSepTransaction txn)
      throws AnchorException {
    boolean txWasUpdated = false;
    boolean txWasCompleted = false;
    boolean shouldClearMessageStatus =
        !StringHelper.isEmpty(patch.getStatus())
            && !isStatusError(patch.getStatus())
            && !StringHelper.isEmpty(txn.getStatus())
            && isStatusError(txn.getStatus());

    if (patch.getStatus() != null && !Objects.equals(txn.getStatus(), patch.getStatus())) {
      validateIfStatusIsSupported(patch.getStatus());
      txWasCompleted =
          !Objects.equals(txn.getStatus(), COMPLETED.getName())
              && Objects.equals(patch.getStatus(), COMPLETED.getName());
      txn.setStatus(patch.getStatus());
      txWasUpdated = true;
    }

    if (patch.getAmountIn() != null
        && (!Objects.equals(txn.getAmountIn(), patch.getAmountIn().getAmount())
            || !Objects.equals(txn.getAmountInAsset(), patch.getAmountIn().getAsset()))) {
      validateAsset("amount_in", patch.getAmountIn());
      txn.setAmountIn(patch.getAmountIn().getAmount());
      txn.setAmountInAsset(patch.getAmountIn().getAsset());
      txWasUpdated = true;
    }

    if (patch.getAmountOut() != null
        && (!Objects.equals(txn.getAmountOut(), patch.getAmountOut().getAmount())
            || !Objects.equals(txn.getAmountOutAsset(), patch.getAmountOut().getAsset()))) {
      validateAsset("amount_out", patch.getAmountOut());
      txn.setAmountOut(patch.getAmountOut().getAmount());
      txn.setAmountOutAsset(patch.getAmountOut().getAsset());
      txWasUpdated = true;
    }

    if (patch.getAmountFee() != null
        && (!Objects.equals(txn.getAmountFee(), patch.getAmountFee().getAmount())
            || !Objects.equals(txn.getAmountFeeAsset(), patch.getAmountFee().getAsset()))) {
      validateAsset("amount_fee", patch.getAmountFee());
      txn.setAmountFee(patch.getAmountFee().getAmount());
      txn.setAmountFeeAsset(patch.getAmountFee().getAsset());
      txWasUpdated = true;
    }

    if (patch.getTransferReceivedAt() != null
        && patch.getTransferReceivedAt().compareTo(txn.getStartedAt()) != 0) {
      if (patch.getTransferReceivedAt().compareTo(txn.getStartedAt()) < 0) {
        throw new BadRequestException(
            String.format(
                "the `transfer_received_at(%s)` cannot be earlier than 'started_at(%s)'",
                patch.getTransferReceivedAt().toString(), txn.getStartedAt().toString()));
      }
      txn.setTransferReceivedAt(patch.getTransferReceivedAt());
      txWasUpdated = true;
    }

    if (patch.getMessage() != null) {
      if (!Objects.equals(txn.getRequiredInfoMessage(), patch.getMessage())) {
        txn.setRequiredInfoMessage(patch.getMessage());
        txWasUpdated = true;
      }
    } else if (shouldClearMessageStatus) {
      txn.setRequiredInfoMessage(null);
    }

    if (patch.getExternalTransactionId() != null
        && !Objects.equals(txn.getExternalTransactionId(), patch.getExternalTransactionId())) {
      txn.setExternalTransactionId(patch.getExternalTransactionId());
      txWasUpdated = true;
    }

    switch (txn.getProtocol()) {
      case "24":
        JdbcSep24Transaction sep24Txn = (JdbcSep24Transaction) txn;
        if (patch.getRefunds() != null) {
          Sep24Refunds updatedRefunds = Sep24Refunds.of(patch.getRefunds(), txn24Store);
          // TODO: validate refunds
          if (!Objects.equals(sep24Txn.getRefunds(), updatedRefunds)) {
            sep24Txn.setRefunds(updatedRefunds);
            txWasUpdated = true;
          }
        }
        break;
      case "31":
        JdbcSep31Transaction sep31Txn = (JdbcSep31Transaction) txn;
        if (patch.getRefunds() != null) {
          Sep31Refunds updatedSep31Refunds = Sep31Refunds.of(patch.getRefunds(), txn31Store);
          // TODO: validate refunds
          if (!Objects.equals(sep31Txn.getRefunds(), updatedSep31Refunds)) {
            sep31Txn.setRefunds(updatedSep31Refunds);
            txWasUpdated = true;
          }
        }
        validateQuoteAndAmounts(sep31Txn);
        break;
    }

    Instant now = Instant.now();
    if (txWasUpdated) {
      txn.setUpdatedAt(now);
    }
    if (txWasCompleted) {
      txn.setCompletedAt(now);
    }
  }

  /**
   * validateIfStatusIsSupported will check if the provided string is a SepTransactionStatus
   * supported by the PlatformAPI
   *
   * @param status a String representing the SepTransactionStatus
   * @throws BadRequestException if the provided status is not supported
   */
  void validateIfStatusIsSupported(String status) throws BadRequestException {
    if (!SepTransactionStatus.isValid(status)) {
      throw new BadRequestException(String.format("invalid status(%s)", status));
    }
  }

  /**
   * validateAsset will validate if the provided amount has valid values and if its asset is
   * supported.
   *
   * @param amount is the object containing the asset full name and the amount.
   * @throws BadRequestException if the provided asset is not supported
   */
  void validateAsset(String fieldName, Amount amount) throws BadRequestException {
    if (amount == null) {
      return;
    }

    // asset amount needs to be non-empty and valid
    SepHelper.validateAmount(fieldName + ".", amount.getAmount());

    // asset name cannot be empty
    if (StringHelper.isEmpty(amount.getAsset())) {
      throw new BadRequestException(fieldName + ".asset cannot be empty");
    }

    // asset name needs to be supported
    if (assets.stream()
        .noneMatch(assetInfo -> assetInfo.getAssetName().equals(amount.getAsset()))) {
      throw new BadRequestException(
          String.format("'%s' is not a supported asset.", amount.getAsset()));
    }
  }

  void validateQuoteAndAmounts(Sep31Transaction txn) throws AnchorException {
    // amount_in = amount_out + amount_fee
    if (StringHelper.isEmpty(txn.getQuoteId())) {
      // without exchange
      if (allAmountAvailable(txn))
        if (decimal(txn.getAmountIn())
                .compareTo(decimal(txn.getAmountOut()).add(decimal(txn.getAmountFee())))
            != 0) throw new BadRequestException("amount_in != amount_out + amount_fee");
    } else {
      // with exchange
      Sep38Quote quote = quoteStore.findByQuoteId(txn.getQuoteId());
      if (quote == null) {
        throw new InternalServerErrorException(
            String.format(
                "invalid quote_id(id=%s) found in transaction(id=%s)",
                txn.getQuoteId(), txn.getId()));
      }

      if (!Objects.equals(txn.getAmountInAsset(), quote.getSellAsset())) {
        throw new BadRequestException("transaction.amount_in_asset != quote.sell_asset");
      }

      if (!equalsAsDecimals(txn.getAmountIn(), quote.getSellAmount())) {
        throw new BadRequestException("transaction.amount_in != quote.sell_amount");
      }

      if (!Objects.equals(txn.getAmountOutAsset(), quote.getBuyAsset())) {
        throw new BadRequestException("transaction.amount_out_asset != quote.buy_asset");
      }

      if (!equalsAsDecimals(txn.getAmountOut(), quote.getBuyAmount())) {
        throw new BadRequestException("transaction.amount_out != quote.buy_amount");
      }

      if (!Objects.equals(txn.getAmountFeeAsset(), quote.getFee().getAsset())) {
        throw new BadRequestException("transaction.amount_fee_asset != quote.fee.asset");
      }

      if (!equalsAsDecimals(txn.getAmountFee(), quote.getFee().getTotal())) {
        throw new BadRequestException("amount_fee != sum(quote.fee.total)");
      }
    }
  }

  GetTransactionResponse toGetTransactionResponse(JdbcSepTransaction txn) throws SepException {
    switch (txn.getProtocol()) {
      case "24":
        return toGetTransactionResponse((JdbcSep24Transaction) txn);
      case "31":
        return toGetTransactionResponse((JdbcSep31Transaction) txn);
      default:
        throw new SepException(String.format("Unsupported protocol:%s", txn.getProtocol()));
    }
  }

  RefundPayment toRefundPayment(
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

  Refunds toRefunds(Sep31Refunds refunds, String assetName) {
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

  private GetTransactionResponse toGetTransactionResponse(JdbcSep31Transaction txn) {
    Refunds refunds = null;
    if (txn.getRefunds() != null) {
      refunds = toRefunds(txn.getRefunds(), txn.getAmountInAsset());
    }

    return GetTransactionResponse.builder()
        .id(txn.getId())
        .sep(31)
        .kind(TransactionEvent.Kind.RECEIVE.getKind())
        .status(txn.getStatus())
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

  RefundPayment toRefundPayment(Sep24RefundPayment refundPayment, String assetName) {
    return RefundPayment.builder()
        .id(refundPayment.getId())
        .idType(RefundPayment.IdType.STELLAR)
        .amount(new Amount(refundPayment.getAmount(), assetName))
        .fee(new Amount(refundPayment.getFee(), assetName))
        .requestedAt(null)
        .refundedAt(null)
        .build();
  }

  Refunds toRefunds(Sep24Refunds refunds, String assetName) {
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

  private GetTransactionResponse toGetTransactionResponse(JdbcSep24Transaction txn) {
    Refunds refunds = null;
    if (txn.getRefunds() != null) {
      refunds = toRefunds(txn.getRefunds(), txn.getAmountInAsset());
    }

    return GetTransactionResponse.builder()
        .id(txn.getId())
        .sep(24)
        .kind(txn.getKind())
        .status(txn.getStatus())
        .amountIn(new Amount(txn.getAmountIn(), txn.getAmountInAsset()))
        .amountOut(new Amount(txn.getAmountOut(), txn.getAmountOutAsset()))
        .amountFee(new Amount(txn.getAmountFee(), txn.getAmountFeeAsset()))
        .startedAt(txn.getStartedAt())
        .updatedAt(txn.getUpdatedAt())
        .completedAt(txn.getCompletedAt())
        .refunds(refunds)
        .stellarTransactions(txn.getStellarTransactions())
        .externalTransactionId(txn.getExternalTransactionId())
        .build();
  }
}
