package org.stellar.anchor.platform.action;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.stellar.anchor.api.sep.AssetInfo.NATIVE_ASSET_CODE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.COMPLETED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.ERROR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.EXPIRED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.REFUNDED;
import static org.stellar.anchor.platform.service.PaymentOperationToEventListener.parsePaymentTime;
import static org.stellar.anchor.platform.utils.TransactionHelper.toGetTransactionResponse;
import static org.stellar.anchor.util.Log.error;
import static org.stellar.anchor.util.Log.errorEx;
import static org.stellar.anchor.util.MathHelper.decimal;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.AmountRequest;
import org.stellar.anchor.api.rpc.action.RpcActionParamsRequest;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.StellarPayment;
import org.stellar.anchor.api.shared.StellarTransaction;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSep31Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.observer.ObservedPayment;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.util.AssetHelper;
import org.stellar.anchor.util.SepHelper;
import org.stellar.anchor.util.StringHelper;
import org.stellar.sdk.AssetTypeCreditAlphaNum;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.operations.PathPaymentBaseOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

public abstract class ActionHandler<T extends RpcActionParamsRequest> {

  protected final Sep24TransactionStore txn24Store;
  protected final Sep31TransactionStore txn31Store;
  private final Validator validator;
  private final Horizon horizon;
  protected final AssetService assetService;
  private final List<AssetInfo> assets;

  public ActionHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    this.txn24Store = txn24Store;
    this.txn31Store = txn31Store;
    this.validator = validator;
    this.horizon = horizon;
    this.assetService = assetService;
    this.assets = assetService.listAllAssets();
  }

  @Transactional
  public GetTransactionResponse handle(Object requestParams) throws AnchorException {
    RpcActionParamsRequest request = (RpcActionParamsRequest) requestParams;
    JdbcSepTransaction txn = getTransaction(request.getTransactionId());

    if (txn == null) {
      throw new InvalidRequestException(
          String.format("Transaction with id[%s] is not found", request.getTransactionId()));
    }

    if (!getSupportedProtocols().contains(txn.getProtocol())) {
      throw new InvalidRequestException(
          String.format(
              "Protocol[%s] is not supported by action[%s]", txn.getProtocol(), getActionType()));
    }

    if (!getSupportedStatuses(txn).contains(SepTransactionStatus.from(txn.getStatus()))) {
      throw new InvalidRequestException(
          String.format(
              "Action[%s] is not supported for status[%s]", getActionType(), txn.getStatus()));
    }

    updateTransaction(txn, request);

    return toGetTransactionResponse(txn, assetService);
  }

  public abstract ActionMethod getActionType();

  protected abstract SepTransactionStatus getNextStatus(JdbcSepTransaction txn, T request)
      throws InvalidRequestException, InvalidParamsException;

  protected abstract Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn);

  protected abstract Set<String> getSupportedProtocols();

  protected abstract void updateTransactionWithAction(JdbcSepTransaction txn, T request)
      throws AnchorException;

  protected JdbcSepTransaction getTransaction(String transactionId) throws AnchorException {
    Sep31Transaction txn31 = txn31Store.findByTransactionId(transactionId);
    if (txn31 != null) {
      return (JdbcSep31Transaction) txn31;
    }
    return (JdbcSep24Transaction) txn24Store.findByTransactionId(transactionId);
  }

  protected void validate(T request) throws InvalidParamsException, InvalidRequestException {
    Set<ConstraintViolation<T>> violations = validator.validate(request);
    if (CollectionUtils.isNotEmpty(violations)) {
      throw new InvalidParamsException(
          violations.stream()
              .map(ConstraintViolation::getMessage)
              .collect(joining(System.lineSeparator())));
    }
  }

  /**
   * validateAsset will validate if the provided amount has valid values and if its asset is
   * supported.
   *
   * @param amount is the object containing the asset full name and the amount.
   */
  protected void validateAsset(String fieldName, AmountRequest amount)
      throws InvalidParamsException {
    validateAsset(fieldName, amount, false);
  }

  protected void validateAsset(String fieldName, AmountRequest amount, boolean allowZero)
      throws InvalidParamsException {
    if (amount == null) {
      return;
    }

    // asset amount needs to be non-empty and valid
    try {
      SepHelper.validateAmount(fieldName + ".", amount.getAmount(), allowZero);
    } catch (BadRequestException e) {
      throw new InvalidParamsException(e.getMessage(), e);
    }

    // asset name cannot be empty
    if (StringHelper.isEmpty(amount.getAsset())) {
      throw new InvalidParamsException(fieldName + ".asset cannot be empty");
    }

    List<AssetInfo> allAssets =
        assets.stream()
            .filter(assetInfo -> assetInfo.getAssetName().equals(amount.getAsset()))
            .collect(toList());

    // asset name needs to be supported
    if (CollectionUtils.isEmpty(allAssets)) {
      throw new InvalidParamsException(
          String.format("'%s' is not a supported asset.", amount.getAsset()));
    }

    if (allAssets.size() == 1) {
      AssetInfo targetAsset = allAssets.get(0);

      if (targetAsset.getSignificantDecimals() != null) {
        // Check that significant decimal is correct
        if (decimal(amount.getAmount(), targetAsset).compareTo(decimal(amount.getAmount())) != 0) {
          throw new InvalidParamsException(
              String.format(
                  "'%s' has invalid significant decimals. Expected: '%s'",
                  amount.getAmount(), targetAsset.getSignificantDecimals()));
        }
      }
    }
  }

  protected boolean isTrustLineConfigured(String account, String asset) {
    try {
      String assetCode = AssetHelper.getAssetCode(asset);
      if (NATIVE_ASSET_CODE.equals(assetCode)) {
        return true;
      }
      String assetIssuer = AssetHelper.getAssetIssuer(asset);

      AccountResponse accountResponse = horizon.getServer().accounts().account(account);
      return Arrays.stream(accountResponse.getBalances())
          .anyMatch(
              balance -> {
                if (balance.getAssetType().equals("credit_alphanum4")
                    || balance.getAssetType().equals("credit_alphanum12")) {
                  AssetTypeCreditAlphaNum creditAsset =
                      (AssetTypeCreditAlphaNum) balance.getAsset().get();
                  return creditAsset.getCode().equals(assetCode)
                      && creditAsset.getIssuer().equals(assetIssuer);
                }
                return false;
              });
    } catch (Exception e) {
      errorEx(
          String.format("Unable to check trust for account[%s] and asset[%s]", account, asset), e);
      return false;
    }
  }

  protected void addStellarTransaction(JdbcSepTransaction txn, String stellarTransactionId) {
    try {
      List<ObservedPayment> payments = getObservedPayments(stellarTransactionId);

      if (!payments.isEmpty()) {
        ObservedPayment firstPayment = payments.get(0);
        ObservedPayment lastPayment = payments.get(payments.size() - 1);
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

        StellarTransaction stellarTransaction =
            StellarTransaction.builder()
                .id(stellarTransactionId)
                .createdAt(parsePaymentTime(firstPayment.getCreatedAt()))
                .memo(txn24.getMemo())
                .memoType(txn24.getMemoType())
                .envelope(firstPayment.getTransactionEnvelope())
                .payments(
                    payments.stream()
                        .map(
                            payment ->
                                StellarPayment.builder()
                                    .id(payment.getId())
                                    .amount(new Amount(payment.getAmount(), payment.getAssetName()))
                                    .sourceAccount(payment.getFrom())
                                    .destinationAccount(payment.getTo())
                                    .paymentType(
                                        payment.getType() == ObservedPayment.Type.PAYMENT
                                            ? StellarPayment.Type.PAYMENT
                                            : StellarPayment.Type.PATH_PAYMENT)
                                    .build())
                        .collect(toList()))
                .build();

        if (txn.getStellarTransactions() == null) {
          txn.setStellarTransactions(List.of(stellarTransaction));
        } else {
          txn.getStellarTransactions().add(stellarTransaction);
        }

        txn.setTransferReceivedAt(parsePaymentTime(lastPayment.getCreatedAt()));
        txn.setStellarTransactionId(stellarTransactionId);
      }
    } catch (Exception ex) {
      errorEx("Failed to retrieve stellar transactions", ex);
    }
  }

  private List<ObservedPayment> getObservedPayments(String stellarTransactionId)
      throws IOException {
    return horizon
        .getServer()
        .payments()
        .includeTransactions(true)
        .forTransaction(stellarTransactionId)
        .execute()
        .getRecords()
        .stream()
        .map(
            operation -> {
              try {
                if (operation instanceof PaymentOperationResponse) {
                  return ObservedPayment.fromPaymentOperationResponse(
                      (PaymentOperationResponse) operation);
                } else if (operation instanceof PathPaymentBaseOperationResponse) {
                  return ObservedPayment.fromPathPaymentOperationResponse(
                      (PathPaymentBaseOperationResponse) operation);
                }
              } catch (SepException e) {
                error("Failed to parse operation response", e);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .sorted(comparing(ObservedPayment::getCreatedAt))
        .collect(toList());
  }

  private void updateTransaction(JdbcSepTransaction txn, RpcActionParamsRequest request)
      throws AnchorException {
    T actionRequest = (T) request;
    validate(actionRequest);

    SepTransactionStatus nextStatus = getNextStatus(txn, actionRequest);

    if (isErrorStatus(nextStatus) && request.getMessage() == null) {
      throw new InvalidParamsException("message is required");
    }

    boolean shouldClearMessageStatus =
        !isErrorStatus(nextStatus) && isErrorStatus(SepTransactionStatus.from(txn.getStatus()));

    updateTransactionWithAction(txn, actionRequest);

    txn.setUpdatedAt(Instant.now());
    txn.setStatus(nextStatus.toString());

    if (isFinalStatus(nextStatus)) {
      txn.setCompletedAt(Instant.now());
    }

    switch (txn.getProtocol()) {
      case "24":
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        if (request.getMessage() != null) {
          txn24.setMessage(request.getMessage());
        } else if (shouldClearMessageStatus) {
          txn24.setMessage(null);
        }
        txn24Store.save(txn24);
        break;
      case "31":
        JdbcSep31Transaction txn31 = (JdbcSep31Transaction) txn;
        if (request.getMessage() != null) {
          txn31.setRequiredInfoMessage(request.getMessage());
        } else if (shouldClearMessageStatus) {
          txn31.setRequiredInfoMessage(null);
        }
        txn31Store.save(txn31);
        break;
    }
  }

  protected boolean isErrorStatus(SepTransactionStatus status) {
    return Set.of(EXPIRED, ERROR).contains(status);
  }

  protected boolean isFinalStatus(SepTransactionStatus status) {
    return Set.of(COMPLETED, REFUNDED).contains(status);
  }
}
