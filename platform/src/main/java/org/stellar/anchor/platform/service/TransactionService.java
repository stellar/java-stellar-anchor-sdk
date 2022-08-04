package org.stellar.anchor.platform.service;

import static org.stellar.anchor.api.sep.SepTransactionStatus.*;
import static org.stellar.anchor.sep31.Sep31Helper.allAmountAvailable;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.isEqualsAsDecimals;

import io.micrometer.core.instrument.Metrics;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.InternalServerErrorException;
import org.stellar.anchor.api.exception.NotFoundException;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.api.platform.PatchTransactionRequest;
import org.stellar.anchor.api.platform.PatchTransactionsRequest;
import org.stellar.anchor.api.platform.PatchTransactionsResponse;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.sep31.RefundsBuilder;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep38.Sep38Quote;
import org.stellar.anchor.sep38.Sep38QuoteStore;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.SepHelper;

@Service
public class TransactionService {
  private final Sep38QuoteStore quoteStore;
  private final Sep31TransactionStore txnStore;
  private final List<AssetInfo> assets;
  static List<String> validStatuses =
      List.of(
          PENDING_STELLAR.getName(),
          PENDING_CUSTOMER_INFO_UPDATE.getName(),
          PENDING_RECEIVER.getName(),
          PENDING_EXTERNAL.getName(),
          COMPLETED.getName(),
          EXPIRED.getName(),
          ERROR.getName());

  TransactionService(
      Sep38QuoteStore quoteStore, Sep31TransactionStore txnStore, AssetService assetService) {
    this.quoteStore = quoteStore;
    this.txnStore = txnStore;
    this.assets = assetService.listAllAssets();
  }

  public GetTransactionResponse getTransaction(String txnId) throws AnchorException {
    if (Objects.toString(txnId, "").isEmpty()) {
      Log.info("Rejecting GET {platformApi}/transaction/:id because the id is empty.");
      throw new BadRequestException("transaction id cannot be empty");
    }

    Sep31Transaction txn = txnStore.findByTransactionId(txnId);
    if (txn == null) {
      throw new NotFoundException(String.format("transaction (id=%s) is not found", txnId));
    }

    return txn.toPlatformApiGetTransactionResponse();
  }

  public PatchTransactionsResponse patchTransactions(PatchTransactionsRequest request)
      throws AnchorException {
    List<PatchTransactionRequest> patchRequests = request.getRecords();
    List<String> ids =
        patchRequests.stream().map(PatchTransactionRequest::getId).collect(Collectors.toList());
    List<? extends Sep31Transaction> fetchedTxns = txnStore.findByTransactionIds(ids);
    Map<String, ? extends Sep31Transaction> sep31Transactions =
        fetchedTxns.stream()
            .collect(Collectors.toMap(Sep31Transaction::getId, Function.identity()));

    List<Sep31Transaction> txnsToSave = new LinkedList<>();
    List<GetTransactionResponse> responses = new LinkedList<>();
    List<Sep31Transaction> statusUpdatedTxns = new LinkedList<>();

    for (PatchTransactionRequest patch : patchRequests) {
      Sep31Transaction txn = sep31Transactions.get(patch.getId());
      if (txn != null) {
        String txnOriginalStatus = txn.getStatus();
        // validate and update the transaction.
        updateSep31Transaction(patch, txn);
        // Add them to the to-be-updated lists.
        txnsToSave.add(txn);
        if (!txnOriginalStatus.equals(txn.getStatus())) {
          statusUpdatedTxns.add(txn);
        }
        responses.add(txn.toPlatformApiGetTransactionResponse());
      } else {
        throw new BadRequestException(String.format("transaction(id=%s) not found", patch.getId()));
      }
    }
    for (Sep31Transaction txn : txnsToSave) {
      // TODO: consider 2-phase commit DB transaction management.
      txnStore.save(txn);
    }
    for (Sep31Transaction txn : statusUpdatedTxns) {
      Metrics.counter(AnchorMetrics.SEP31_TRANSACTION.toString(), "status", txn.getStatus())
          .increment();
    }
    return new PatchTransactionsResponse(responses);
  }

  /**
   * updateSep31Transaction will inject the new values from the PatchTransactionRequest into the
   * database Sep31Transaction and validate them to make sure they are compliant.
   *
   * @param ptr is the PatchTransactionRequest containing the updated values.
   * @param txn is the Sep31Transaction stored in the database that needs to be updated.
   * @throws AnchorException if any of the new values is invalid or non-compliant.
   */
  void updateSep31Transaction(PatchTransactionRequest ptr, Sep31Transaction txn)
      throws AnchorException {
    if (ptr.getStatus() != null) {
      validateIfStatusIsSupported(ptr.getStatus());
      txn.setStatus(ptr.getStatus());
    }
    if (ptr.getAmountIn() != null) {
      validateAsset("amount_in", ptr.getAmountIn());
      txn.setAmountIn(ptr.getAmountIn().getAmount());
      txn.setAmountInAsset(ptr.getAmountIn().getAsset());
    }
    if (ptr.getAmountOut() != null) {
      validateAsset("amount_out", ptr.getAmountOut());
      txn.setAmountOut(ptr.getAmountOut().getAmount());
      txn.setAmountOutAsset(ptr.getAmountOut().getAsset());
    }
    if (ptr.getAmountFee() != null) {
      validateAsset("amount_fee", ptr.getAmountFee());
      txn.setAmountFee(ptr.getAmountFee().getAmount());
      txn.setAmountFeeAsset(ptr.getAmountFee().getAsset());
    }
    if (ptr.getTransferReceivedAt() != null) {
      if (ptr.getTransferReceivedAt().compareTo(txn.getStartedAt()) < 0) {
        throw new BadRequestException(
            String.format(
                "the `transfer_received_at(%s)` cannot be earlier than 'started_at(%s)'",
                ptr.getTransferReceivedAt().toString(), txn.getStartedAt().toString()));
      }
      txn.setTransferReceivedAt(ptr.getTransferReceivedAt());
    }
    if (ptr.getMessage() != null) {
      txn.setRequiredInfoMessage(ptr.getMessage());
    }
    if (ptr.getRefunds() != null) {
      txn.setRefunds(new RefundsBuilder(txnStore).fromPlatformApiRefunds(ptr.getRefunds()));
      // TODO: validate refunds
    }
    if (ptr.getExternalTransactionId() != null) {
      txn.setExternalTransactionId(ptr.getExternalTransactionId());
    }

    validateQuoteAndAmounts(txn);
  }

  /**
   * validateIfStatusIsSupported will check if the provided string is a SepTransactionStatus
   * supported by the PlatformAPI
   *
   * @param status a String representing the SepTransactionStatus
   * @throws BadRequestException if the provided status is not supported
   */
  void validateIfStatusIsSupported(String status) throws BadRequestException {
    if (!validStatuses.contains(status)) {
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
    if (Objects.toString(amount.getAsset(), "").isEmpty()) {
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
    if (Objects.toString(txn.getQuoteId(), "").isEmpty()) {
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

      if (!isEqualsAsDecimals(txn.getAmountIn(), quote.getSellAmount())) {
        throw new BadRequestException("transaction.amount_in != quote.sell_amount");
      }

      if (!Objects.equals(txn.getAmountOutAsset(), quote.getBuyAsset())) {
        throw new BadRequestException("transaction.amount_out_asset != quote.buy_asset");
      }

      if (!isEqualsAsDecimals(txn.getAmountOut(), quote.getBuyAmount())) {
        throw new BadRequestException("transaction.amount_out != quote.buy_amount");
      }

      if (!Objects.equals(txn.getAmountFeeAsset(), quote.getFee().getAsset())) {
        throw new BadRequestException("transaction.amount_fee_asset != quote.fee.asset");
      }

      if (!isEqualsAsDecimals(txn.getAmountFee(), quote.getFee().getTotal())) {
        throw new BadRequestException("amount_fee != sum(quote.fee.total)");
      }
    }
  }
}
