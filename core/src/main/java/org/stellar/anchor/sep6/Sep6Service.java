package org.stellar.anchor.sep6;

import static org.stellar.sdk.xdr.MemoType.MEMO_HASH;

import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.sep.sep6.*;
import org.stellar.anchor.api.sep.sep6.InfoResponse.*;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.config.Sep6Config;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.anchor.util.SepHelper;
import org.stellar.anchor.util.TransactionHelper;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Memo;

public class Sep6Service {
  private final Sep6Config sep6Config;
  private final AssetService assetService;
  private final Sep6TransactionStore txnStore;
  private final EventService.Session eventSession;

  private final InfoResponse infoResponse;

  public Sep6Service(
      Sep6Config sep6Config,
      AssetService assetService,
      Sep6TransactionStore txnStore,
      EventService eventService) {
    this.sep6Config = sep6Config;
    this.assetService = assetService;
    this.txnStore = txnStore;
    this.eventSession =
        eventService.createSession(this.getClass().getName(), EventService.EventQueue.TRANSACTION);
    this.infoResponse = buildInfoResponse();
  }

  public InfoResponse getInfo() {
    return infoResponse;
  }

  public GetDepositResponse deposit(Sep10Jwt token, GetDepositRequest request)
      throws AnchorException {
    // Pre-validation
    if (token == null) {
      throw new SepNotAuthorizedException("missing token");
    }
    if (request == null) {
      throw new SepValidationException("missing request");
    }

    AssetInfo asset = assetService.getAsset(request.getAssetCode());
    if (asset == null || !asset.getDeposit().getEnabled() || !asset.getSep6Enabled()) {
      throw new SepValidationException(
          String.format("invalid operation for asset %s", request.getAssetCode()));
    }

    try {
      KeyPair.fromAccountId(request.getAccount());
    } catch (RuntimeException ex) {
      throw new SepValidationException(String.format("invalid account %s", request.getAccount()));
    }
    Memo memo = MemoHelper.makeMemo(request.getMemo(), request.getMemoType());
    String id = SepHelper.generateSepTransactionId();

    Sep6TransactionBuilder builder =
        new Sep6TransactionBuilder(txnStore)
            .id(id)
            .transactionId(id)
            .status(SepTransactionStatus.INCOMPLETE.toString())
            .kind(Sep6Transaction.Kind.DEPOSIT.toString())
            .type(request.getType())
            .assetCode(request.getAssetCode())
            .assetIssuer(asset.getIssuer())
            .amountExpected(request.getAmount())
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .toAccount(request.getAccount());

    if (memo != null) {
      builder.memo(memo.toString());
      builder.memoType(SepHelper.memoTypeString(MemoHelper.memoType(memo)));
    }

    Sep6Transaction txn = builder.build();
    txnStore.save(txn);

    eventSession.publish(
        AnchorEvent.builder()
            .id(UUID.randomUUID().toString())
            .sep("6")
            .type(AnchorEvent.Type.TRANSACTION_CREATED)
            .transaction(TransactionHelper.toGetTransactionResponse(txn, assetService))
            .build());

    return GetDepositResponse.builder()
        .how("Check the transaction for more information about how to deposit.")
        .id(txn.getId())
        .build();
  }

  public GetWithdrawResponse withdraw(Sep10Jwt token, GetWithdrawRequest request)
      throws AnchorException {
    // Pre-validation
    if (token == null) {
      throw new SepNotAuthorizedException("missing token");
    }
    if (request == null) {
      throw new SepValidationException("missing request");
    }

    AssetInfo asset = assetService.getAsset(request.getAssetCode());
    if (asset == null || !asset.getWithdraw().getEnabled() || !asset.getSep6Enabled()) {
      throw new SepValidationException(
          String.format("invalid operation for asset %s", request.getAssetCode()));
    }
    if (!asset.getWithdraw().getMethods().contains(request.getType())) {
      throw new SepValidationException(
          String.format(
              "invalid type %s for asset %s, supported types are %s",
              request.getType(), request.getAssetCode(), asset.getWithdraw().getMethods()));
    }
    BigDecimal amount = new BigDecimal(request.getAmount());
    if (amount.scale() > asset.getSignificantDecimals()) {
      throw new SepValidationException(
          String.format(
              "invalid amount %s for asset %s, significant decimals is %s",
              request.getAmount(), request.getAssetCode(), asset.getSignificantDecimals()));
    }
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new SepValidationException(
          String.format(
              "invalid amount %s for asset %s", request.getAmount(), request.getAssetCode()));
    }

    String id = SepHelper.generateSepTransactionId();

    // Make a unique memo from the transaction ID
    String memo = StringUtils.truncate(id, 32);
    memo = StringUtils.leftPad(memo, 32, '0');
    memo = new String(Base64.getEncoder().encode(memo.getBytes()));

    Sep6TransactionBuilder builder =
        new Sep6TransactionBuilder(txnStore)
            .id(id)
            .transactionId(id)
            .status(SepTransactionStatus.INCOMPLETE.toString())
            .kind(Sep6Transaction.Kind.WITHDRAWAL.toString())
            .type(request.getType())
            .assetCode(request.getAssetCode())
            .assetIssuer(asset.getIssuer())
            .amountExpected(request.getAmount())
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .memo(memo)
            .memoType(MemoHelper.memoTypeAsString(MEMO_HASH))
            .fromAccount(token.getAccount())
            .withdrawAnchorAccount(asset.getDistributionAccount())
            .toAccount(asset.getDistributionAccount())
            .refundMemo(request.getRefundMemo())
            .refundMemoType(request.getRefundMemoType());

    Sep6Transaction txn = builder.build();
    txnStore.save(txn);

    eventSession.publish(
        AnchorEvent.builder()
            .id(UUID.randomUUID().toString())
            .sep("6")
            .type(AnchorEvent.Type.TRANSACTION_CREATED)
            .transaction(TransactionHelper.toGetTransactionResponse(txn, assetService))
            .build());

    return GetWithdrawResponse.builder()
        .accountId(asset.getDistributionAccount())
        .id(txn.getId())
        .memo(memo)
        .memoType(MemoHelper.memoTypeAsString(MEMO_HASH))
        .build();
  }

  public GetTransactionsResponse findTransactions(Sep10Jwt token, GetTransactionsRequest request)
      throws SepException {
    // Pre-validation
    if (token == null) {
      throw new SepNotAuthorizedException("missing token");
    }
    if (request == null) {
      throw new SepValidationException("missing request");
    }
    if (!request.getAccount().equals(token.getAccount())) {
      throw new SepNotAuthorizedException("account does not match token");
    }
    if (assetService.getAsset(request.getAssetCode()) == null) {
      throw new SepValidationException(
          String.format("asset code %s not supported", request.getAssetCode()));
    }

    // Query the transaction store
    List<Sep6Transaction> transactions =
        txnStore.findTransactions(token.getAccount(), token.getAccountMemo(), request);
    List<org.stellar.anchor.api.sep.sep6.Sep6Transaction> responses =
        transactions.stream().map(Sep6TransactionUtils::fromTxn).collect(Collectors.toList());

    return new GetTransactionsResponse(responses);
  }

  public GetTransactionResponse findTransaction(Sep10Jwt token, GetTransactionRequest request)
      throws AnchorException {
    // Pre-validation
    if (token == null) {
      throw new SepNotAuthorizedException("missing token");
    }
    if (request == null) {
      throw new SepValidationException("missing request");
    }

    // Query the transaction store
    Sep6Transaction txn;
    if (request.getId() != null) {
      txn = txnStore.findByTransactionId(request.getId());
    } else if (request.getStellarTransactionId() != null) {
      txn = txnStore.findByStellarTransactionId(request.getStellarTransactionId());
    } else if (request.getExternalTransactionId() != null) {
      txn = txnStore.findByExternalTransactionId(request.getExternalTransactionId());
    } else {
      throw new SepValidationException(
          "One of id, stellar_transaction_id, or external_transaction_id is required");
    }

    // Validate the transaction
    if (txn == null) {
      throw new NotFoundException("transaction not found");
    }
    if (!Objects.equals(txn.getSep10Account(), token.getAccount())) {
      throw new NotFoundException("account does not match token");
    }
    if (!Objects.equals(txn.getSep10AccountMemo(), token.getAccountMemo())) {
      throw new NotFoundException("account memo does not match token");
    }

    return new GetTransactionResponse(Sep6TransactionUtils.fromTxn(txn));
  }

  private InfoResponse buildInfoResponse() {
    InfoResponse response =
        InfoResponse.builder()
            .deposit(new HashMap<>())
            .depositExchange(new HashMap<>())
            .withdraw(new HashMap<>())
            .withdrawExchange(new HashMap<>())
            .fee(
                FeeResponse.builder()
                    .enabled(false)
                    .description("Fee endpoint is not supported.")
                    .build())
            .transactions(
                TransactionsResponse.builder().enabled(true).authenticationRequired(true).build())
            .transaction(
                TransactionResponse.builder().enabled(true).authenticationRequired(true).build())
            .features(
                FeaturesResponse.builder()
                    .accountCreation(sep6Config.getFeatures().isAccountCreation())
                    .claimableBalances(sep6Config.getFeatures().isClaimableBalances())
                    .build())
            .build();

    for (AssetInfo asset : assetService.listAllAssets()) {
      if (asset.getSep6Enabled()) {

        if (asset.getDeposit().getEnabled()) {
          List<String> methods = asset.getDeposit().getMethods();
          AssetInfo.Field type =
              AssetInfo.Field.builder()
                  .description("type of deposit to make")
                  .choices(methods)
                  .build();

          DepositAssetResponse deposit =
              DepositAssetResponse.builder()
                  .enabled(true)
                  .authenticationRequired(true)
                  .fields(ImmutableMap.of("type", type))
                  .build();

          response.getDeposit().put(asset.getCode(), deposit);
          response.getDepositExchange().put(asset.getCode(), deposit);
        }

        if (asset.getWithdraw().getEnabled()) {
          List<String> methods = asset.getWithdraw().getMethods();
          Map<String, Map<String, AssetInfo.Field>> types = new HashMap<>();
          for (String method : methods) {
            types.put(method, new HashMap<>());
          }

          WithdrawAssetResponse withdraw =
              WithdrawAssetResponse.builder()
                  .enabled(true)
                  .authenticationRequired(true)
                  .types(types)
                  .build();

          response.getWithdraw().put(asset.getCode(), withdraw);
          response.getWithdrawExchange().put(asset.getCode(), withdraw);
        }
      }
    }
    return response;
  }
}
