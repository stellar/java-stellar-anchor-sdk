package org.stellar.anchor.platform.service;

import static org.stellar.anchor.api.event.AnchorEvent.Type.TRANSACTION_STATUS_CHANGED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.*;
import static org.stellar.anchor.platform.utils.TransactionHelper.toGetTransactionResponse;
import static org.stellar.anchor.sep31.Sep31Helper.allAmountAvailable;
import static org.stellar.anchor.util.BeanHelper.updateField;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.equalsAsDecimals;
import static org.stellar.anchor.util.MemoHelper.memoTypeAsString;
import static org.stellar.sdk.xdr.MemoType.MEMO_HASH;

import io.micrometer.core.instrument.Metrics;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.InternalServerErrorException;
import org.stellar.anchor.api.exception.NotFoundException;
import org.stellar.anchor.api.platform.*;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSep31Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
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

public class TransactionService {
  private final Sep38QuoteStore quoteStore;
  private final Sep31TransactionStore txn31Store;
  private final Sep24TransactionStore txn24Store;
  private final List<AssetInfo> assets;
  private final EventService eventService;
  private final AssetService assetService;

  static boolean isStatusError(String status) {
    return List.of(PENDING_CUSTOMER_INFO_UPDATE.getStatus(), EXPIRED.getStatus(), ERROR.getStatus())
        .contains(status);
  }

  public TransactionService(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Sep38QuoteStore quoteStore,
      AssetService assetService,
      EventService eventService) {
    this.txn24Store = txn24Store;
    this.txn31Store = txn31Store;
    this.quoteStore = quoteStore;
    this.assets = assetService.listAllAssets();
    this.eventService = eventService;
    this.assetService = assetService;
  }

  /**
   * Fetch the transaction and convert the transaction to an object of Sep24GetTransactionResponse
   * class.
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
      return toGetTransactionResponse(txn, assetService);
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

    List<GetTransactionResponse> txnResponses = new LinkedList<>();
    for (PatchTransactionRequest patchRequest : patchRequests) {
      txnResponses.add(patchTransaction(patchRequest));
    }
    return new PatchTransactionsResponse(txnResponses);
  }

  private GetTransactionResponse patchTransaction(PatchTransactionRequest patch)
      throws AnchorException {
    JdbcSepTransaction txn = findTransaction(patch.getTransaction().getId());
    if (txn == null)
      throw new BadRequestException(
          String.format("transaction(id=%s) not found", patch.getTransaction().getId()));

    String lastStatus = txn.getStatus();
    // TODO - jamie to validate request before using values
    updateSepTransaction(patch.getTransaction(), txn);
    switch (txn.getProtocol()) {
      case "24":
        JdbcSep24Transaction sep24Transaction = (JdbcSep24Transaction) txn;
        // add a memo for the transaction if the transaction is ready for user to send funds
        if (sep24Transaction.getMemo() == null
            && sep24Transaction.getStatus().equals(PENDING_USR_TRANSFER_START.toString())) {
          // TODO - move this to separate file ex: Sep31DepositInfoGeneratorSelf.java
          String memo = StringUtils.truncate(sep24Transaction.getId(), 32);
          memo = StringUtils.leftPad(memo, 32, '0');
          memo = new String(Base64.getEncoder().encode(memo.getBytes()));
          sep24Transaction.setMemo(memo);
          sep24Transaction.setMemoType(memoTypeAsString(MEMO_HASH));
        }
        txn24Store.save(sep24Transaction);
        eventService.publish(sep24Transaction, TRANSACTION_STATUS_CHANGED);
        break;
      case "31":
        txn31Store.save((JdbcSep31Transaction) txn);
        eventService.publish((JdbcSep31Transaction) txn, TRANSACTION_STATUS_CHANGED);
        break;
    }
    if (!lastStatus.equals(txn.getStatus())) updateMetrics(txn);
    return toGetTransactionResponse(txn, assetService);
  }

  void updateMetrics(JdbcSepTransaction txn) {
    switch (txn.getProtocol()) {
      case "24":
        Metrics.counter(AnchorMetrics.SEP24_TRANSACTION.toString(), "status", txn.getStatus())
            .increment();
        break;
      case "31":
        Metrics.counter(AnchorMetrics.SEP31_TRANSACTION.toString(), "status", txn.getStatus())
            .increment();
        break;
    }
  }

  void updateSepTransaction(PlatformTransactionData patch, JdbcSepTransaction txn)
      throws AnchorException {
    boolean txnUpdated = false;
    boolean shouldClearMessageStatus =
        !StringHelper.isEmpty(patch.getStatus().getStatus())
            && !isStatusError(patch.getStatus().getStatus())
            && !StringHelper.isEmpty(txn.getStatus())
            && isStatusError(txn.getStatus());
    // update status
    txnUpdated = updateField(patch, "status.status", txn, "status", txnUpdated);
    // update amount_in
    txnUpdated = updateField(patch, "amountIn.amount", txn, "amountIn", txnUpdated);
    txnUpdated = updateField(patch, "amountIn.asset", txn, "amountInAsset", txnUpdated);
    // update amount_out
    txnUpdated = updateField(patch, "amountOut.amount", txn, "amountOut", txnUpdated);
    txnUpdated = updateField(patch, "amountOut.asset", txn, "amountOutAsset", txnUpdated);
    // update amount_fee
    txnUpdated = updateField(patch, "amountFee.amount", txn, "amountFee", txnUpdated);
    txnUpdated = updateField(patch, "amountFee.asset", txn, "amountFeeAsset", txnUpdated);
    // update starte_at, completed_at, updated_at, transferReceivedAt
    txnUpdated = updateField(patch, txn, "startedAt", txnUpdated);
    txnUpdated = updateField(patch, txn, "updatedAt", txnUpdated);
    txnUpdated = updateField(patch, txn, "completedAt", txnUpdated);
    txnUpdated = updateField(patch, txn, "transferReceivedAt", txnUpdated);
    // update external_transaction_id
    txnUpdated = updateField(patch, txn, "externalTransactionId", txnUpdated);
    // update stellar_transactions
    txnUpdated = updateField(patch, txn, "stellarTransactions", txnUpdated);

    switch (txn.getProtocol()) {
      case "24":
        JdbcSep24Transaction sep24Txn = (JdbcSep24Transaction) txn;

        txnUpdated = updateField(patch, sep24Txn, "message", txnUpdated);
        txnUpdated = updateField(patch, sep24Txn, "memo", txnUpdated);
        txnUpdated = updateField(patch, sep24Txn, "memoType", txnUpdated);
        txnUpdated = updateField(patch, sep24Txn, "kycVerified", txnUpdated);

        // update refunds
        if (patch.getRefunds() != null) {
          Sep24Refunds updatedRefunds = Sep24Refunds.of(patch.getRefunds(), txn24Store);
          if (!Objects.equals(sep24Txn.getRefunds(), updatedRefunds)) {
            sep24Txn.setRefunds(updatedRefunds);
            txnUpdated = true;
          }
        }
        break;
      case "31":
        // update message
        if (shouldClearMessageStatus) {
          JdbcSep31Transaction sep31Txn = (JdbcSep31Transaction) txn;
          sep31Txn.setRequiredInfoMessage(null);
        } else {
          txnUpdated = updateField(patch, "message", txn, "requiredInfoMessage", txnUpdated);
        }

        JdbcSep31Transaction sep31Txn = (JdbcSep31Transaction) txn;
        // update sender and receiver
        txnUpdated = updateField(patch, "customers.sender", txn, "senderId", txnUpdated);
        txnUpdated = updateField(patch, "customers.receiver", txn, "receiverId", txnUpdated);
        // update creator
        txnUpdated = updateField(patch, txn, "creator", txnUpdated);
        // update refunds
        if (patch.getRefunds() != null) {
          Sep31Refunds updatedSep31Refunds = Sep31Refunds.of(patch.getRefunds(), txn31Store);
          if (!Objects.equals(sep31Txn.getRefunds(), updatedSep31Refunds)) {
            sep31Txn.setRefunds(updatedSep31Refunds);
            txnUpdated = true;
          }
        }

        validateQuoteAndAmounts(sep31Txn);
        break;
    }

    Instant now = Instant.now();
    if (txnUpdated) {
      txn.setUpdatedAt(now);
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
}
