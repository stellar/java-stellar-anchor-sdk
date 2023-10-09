package org.stellar.anchor.platform.rpc;

import static org.stellar.anchor.api.event.AnchorEvent.Type.TRANSACTION_STATUS_CHANGED;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.RECEIVE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.COMPLETED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.ERROR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.EXPIRED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.REFUNDED;
import static org.stellar.anchor.event.EventService.EventQueue.TRANSACTION;
import static org.stellar.anchor.platform.utils.PlatformTransactionHelper.toGetTransactionResponse;
import static org.stellar.anchor.util.MetricConstants.*;

import com.google.gson.Gson;
import io.micrometer.core.instrument.Metrics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.rpc.method.RpcMethodParamsRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.event.EventService.Session;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSep31Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.util.GsonUtils;

public abstract class RpcMethodHandler<T extends RpcMethodParamsRequest> {

  private static final Gson gson = GsonUtils.getInstance();

  protected final Sep24TransactionStore txn24Store;
  protected final Sep31TransactionStore txn31Store;
  protected final AssetService assetService;
  private final RequestValidator requestValidator;
  private final Class<T> requestType;
  private final Session eventSession;

  public RpcMethodHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      Class<T> requestType) {
    this.txn24Store = txn24Store;
    this.txn31Store = txn31Store;
    this.requestValidator = requestValidator;
    this.assetService = assetService;
    this.requestType = requestType;
    this.eventSession = eventService.createSession(this.getClass().getName(), TRANSACTION);
  }

  @Transactional
  public GetTransactionResponse handle(Object requestParams) throws AnchorException {
    T request = gson.fromJson(gson.toJson(requestParams), requestType);
    JdbcSepTransaction txn = getTransaction(request.getTransactionId());

    if (txn == null) {
      throw new InvalidRequestException(
          String.format("Transaction with id[%s] is not found", request.getTransactionId()));
    }

    if (!getSupportedStatuses(txn).contains(SepTransactionStatus.from(txn.getStatus()))) {
      String kind;
      switch (Sep.from(txn.getProtocol())) {
        case SEP_24:
          JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
          kind = txn24.getKind();
          break;
        case SEP_31:
          kind = RECEIVE.getKind();
          break;
        default:
          kind = null;
      }
      throw new InvalidRequestException(
          String.format(
              "RPC method[%s] is not supported. Status[%s], kind[%s], protocol[%s], funds received[%b]",
              getRpcMethod(), txn.getStatus(), kind, txn.getProtocol(), areFundsReceived(txn)));
    }

    GetTransactionResponse txResponse = toGetTransactionResponse(txn, assetService);
    boolean isTxnUpdated = updateTransaction(txn, request);

    if (isTxnUpdated) {
      eventSession.publish(
          AnchorEvent.builder()
              .id(UUID.randomUUID().toString())
              .sep(txn.getProtocol())
              .type(TRANSACTION_STATUS_CHANGED)
              .transaction(txResponse)
              .build());
    }
    return txResponse;
  }

  public abstract RpcMethod getRpcMethod();

  protected abstract SepTransactionStatus getNextStatus(JdbcSepTransaction txn, T request)
      throws InvalidRequestException, InvalidParamsException;

  protected abstract Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn);

  protected abstract void updateTransactionWithRpcRequest(JdbcSepTransaction txn, T request)
      throws AnchorException;

  protected JdbcSepTransaction getTransaction(String transactionId) throws AnchorException {
    Sep31Transaction txn31 = txn31Store.findByTransactionId(transactionId);
    if (txn31 != null) {
      return (JdbcSep31Transaction) txn31;
    }
    return (JdbcSep24Transaction) txn24Store.findByTransactionId(transactionId);
  }

  protected void validate(JdbcSepTransaction txn, T request)
      throws InvalidParamsException, InvalidRequestException, BadRequestException {
    requestValidator.validate(request);
  }

  /*
   * Returns true if the transaction was updated.
   * Returns false if the transaction was not updated.
   */
  private boolean updateTransaction(JdbcSepTransaction txn, T request) throws AnchorException {
    validate(txn, request);

    SepTransactionStatus nextStatus = getNextStatus(txn, request);

    if (isErrorStatus(nextStatus) && request.getMessage() == null) {
      throw new InvalidParamsException("message is required");
    }

    boolean shouldClearMessageStatus =
        !isErrorStatus(nextStatus) && isErrorStatus(SepTransactionStatus.from(txn.getStatus()));

    byte[] serialized1 = snapshotTransaction(txn);
    updateTransactionWithRpcRequest(txn, request);
    byte[] serialized2 = snapshotTransaction(txn);

    txn.setUpdatedAt(Instant.now());
    txn.setStatus(nextStatus.toString());

    if (isFinalStatus(nextStatus)) {
      txn.setCompletedAt(Instant.now());
    }

    switch (Sep.from(txn.getProtocol())) {
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        if (request.getMessage() != null) {
          txn24.setMessage(request.getMessage());
        } else if (shouldClearMessageStatus) {
          txn24.setMessage(null);
        }
        txn24Store.save(txn24);
        break;
      case SEP_31:
        JdbcSep31Transaction txn31 = (JdbcSep31Transaction) txn;
        if (request.getMessage() != null) {
          txn31.setRequiredInfoMessage(request.getMessage());
        } else if (shouldClearMessageStatus) {
          txn31.setRequiredInfoMessage(null);
        }
        txn31Store.save(txn31);
        break;
    }

    updateMetrics(txn);
    return !isEqual(serialized1, serialized2);
  }

  protected boolean areFundsReceived(JdbcSepTransaction txn) {
    return txn.getTransferReceivedAt() != null;
  }

  protected boolean isErrorStatus(SepTransactionStatus status) {
    return Set.of(EXPIRED, ERROR).contains(status);
  }

  protected boolean isFinalStatus(SepTransactionStatus status) {
    return Set.of(COMPLETED, REFUNDED).contains(status);
  }

  private void updateMetrics(JdbcSepTransaction txn) {
    switch (Sep.from(txn.getProtocol())) {
      case SEP_24:
        Metrics.counter(PLATFORM_RPC_TRANSACTION, SEP, TV_SEP24).increment();
        break;
      case SEP_31:
        Metrics.counter(PLATFORM_RPC_TRANSACTION, SEP, TV_SEP31).increment();
        break;
    }
  }

  private boolean isEqual(byte[] serialized1, byte[] serialized2) {
    return serialized1 != null && serialized2 != null && Arrays.equals(serialized1, serialized2);
  }

  private byte[] snapshotTransaction(JdbcSepTransaction txn) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(txn);
      byte[] serialized1 = baos.toByteArray();
      oos.close();
      return serialized1;
    } catch (IOException e) {
      return null;
    }
  }
}
