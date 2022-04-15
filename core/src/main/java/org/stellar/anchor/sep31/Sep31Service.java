package org.stellar.anchor.sep31;

import static org.stellar.anchor.dto.sep31.Sep31InfoResponse.AssetResponse;
import static org.stellar.anchor.sep31.Sep31Helper.amountEquals;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MemoHelper.memoType;
import static org.stellar.anchor.util.SepHelper.*;
import static org.stellar.sdk.xdr.MemoType.MEMO_HASH;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.stellar.anchor.asset.AssetInfo;
import org.stellar.anchor.asset.AssetInfo.Sep31TxnFieldSpecs;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep31Config;
import org.stellar.anchor.dto.sep12.Sep12GetCustomerRequest;
import org.stellar.anchor.dto.sep12.Sep12GetCustomerResponse;
import org.stellar.anchor.dto.sep31.*;
import org.stellar.anchor.dto.sep31.Sep31GetTransactionResponse.TransactionResponse;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.event.models.Amount;
import org.stellar.anchor.event.models.StellarId;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.exception.BadRequestException;
import org.stellar.anchor.exception.NotFoundException;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import org.stellar.anchor.integration.fee.FeeIntegration;
import org.stellar.anchor.model.Sep31Transaction;
import org.stellar.anchor.model.Sep31TransactionBuilder;
import org.stellar.anchor.model.Sep38Quote;
import org.stellar.anchor.model.TransactionStatus;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.sep38.Sep38QuoteStore;
import org.stellar.platform.apis.callbacks.requests.GetFeeRequest;
import org.stellar.platform.apis.callbacks.responses.GetFeeResponse;

public class Sep31Service {
  private final AppConfig appConfig;
  private final Sep31Config sep31Config;
  private final Sep31TransactionStore sep31TransactionStore;
  private final Sep38QuoteStore sep38QuoteStore;
  private final AssetService assetService;
  private FeeIntegration feeIntegration;
  private final CustomerIntegration customerIntegration;
  private Sep31InfoResponse infoResponse;
  final EventService eventService;

  public Sep31Service(
      AppConfig appConfig,
      Sep31Config sep31Config,
      Sep31TransactionStore sep31TransactionStore,
      Sep38QuoteStore sep38QuoteStore,
      AssetService assetService,
      FeeIntegration feeIntegration,
      CustomerIntegration customerIntegration,
      EventService eventService) {
    this.appConfig = appConfig;
    this.sep31Config = sep31Config;
    this.sep31TransactionStore = sep31TransactionStore;
    this.sep38QuoteStore = sep38QuoteStore;
    this.assetService = assetService;
    this.feeIntegration = feeIntegration;
    this.customerIntegration = customerIntegration;
    this.eventService = eventService;
    this.infoResponse = createFromAssets(assetService.listAllAssets());
  }

  public Sep31InfoResponse getInfo() {
    return infoResponse;
  }

  public Sep31PostTransactionResponse postTransaction(
      JwtToken jwtToken, Sep31PostTransactionRequest request) throws AnchorException {

    validateAmount("", request.getAmount());
    validateLanguage(appConfig, request.getLang());
    validateRequiredFields(request.getAssetCode(), request.getFields().getTransaction());
    AssetInfo asset = validateAsset(request);
    validateSenderAndReceiver(request);

    Sep31Transaction txn =
        new Sep31TransactionBuilder(sep31TransactionStore)
            .id(generateSepTransactionId())
            .status(TransactionStatus.PENDING_SENDER.toString())
            .stellarAccountId(jwtToken.getAccount())
            .amountInAsset(request.getAssetCode())
            .amountIn(request.getAmount())
            .clientDomain(jwtToken.getClientDomain())
            .fields(request.getFields().getTransaction())
            .build();

    validateKyc(request.getSenderId(), request.getReceiverId());
    validateAndApplyFeeAndQuote(txn, request, jwtToken);
    generateTransactionMemo(txn);

    sep31TransactionStore.save(txn);

    TransactionEvent event =
        TransactionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .type(TransactionEvent.Type.TRANSACTION_CREATED)
            .id(txn.getId())
            .sep(TransactionEvent.Sep.SEP_31)
            .kind(TransactionEvent.Kind.RECEIVE)
            .amountIn(
                Amount.builder().amount(txn.getAmountIn()).asset(txn.getAmountInAsset()).build())
            .amountOut(
                Amount.builder().amount(txn.getAmountOut()).asset(txn.getAmountInAsset()).build())
            .amountFee(
                Amount.builder().amount(txn.getAmountFee()).asset(txn.getAmountInAsset()).build())
            .quoteId(txn.getQuoteId())
            .startedAt(txn.getStartedAt())
            .sourceAccount(request.getSenderId())
            .destinationAccount(request.getReceiverId())
            .creator(
                StellarId.builder()
                    .account(txn.getStellarAccountId())
                    .memo(txn.getStellarMemo())
                    .memoType(txn.getStellarMemoType())
                    .build())
            .build();

    eventService.publish(event);

    return Sep31PostTransactionResponse.builder()
        .id(txn.getId())
        .stellarAccountId(asset.getDistributionAccount())
        .stellarMemo(txn.getStellarMemo())
        .stellarMemoType(txn.getStellarMemoType())
        .build();
  }

  private void generateTransactionMemo(Sep31Transaction txn) throws SepException {
    String memo = StringUtils.truncate(txn.getId(), 32);
    memo = StringUtils.leftPad(memo, 32, '0');
    memo = new String(Base64.getEncoder().encode(memo.getBytes()));
    txn.setStellarMemo(memo);
    txn.setStellarMemoType(memoType(MEMO_HASH));
  }

  public Sep31GetTransactionResponse getTransaction(String id)
      throws SepException, NotFoundException, BadRequestException {
    if (id == null) {
      throw new BadRequestException("'id' is not provided");
    }
    Sep31Transaction txn = sep31TransactionStore.findByTransactionId(id);
    if (txn == null) {
      throw new NotFoundException(String.format("transaction (id=%s) not found", id));
    }

    return fromTransactionToResponse(txn);
  }

  public Sep31GetTransactionResponse patchTransaction(Sep31PatchTransactionRequest request)
      throws AnchorException {
    Sep31Transaction txn = sep31TransactionStore.findByTransactionId(request.getId());
    if (txn == null) {
      throw new NotFoundException(String.format("transaction (id=%s) not found", request.getId()));
    }

    // validate if the transaction is in the pending_transaction_info_update status
    if (!Objects.equals(
        txn.getStatus(), TransactionStatus.PENDING_TRANSACTION_INFO_UPDATE.toString())) {
      throw new BadRequestException(
          String.format("transaction (id=%s) does not need update", txn.getId()));
    }

    validatePatchTransactionFields(txn, request);

    request
        .getFields()
        .getTransaction()
        .forEach(
            (fieldName, fieldValue) -> {
              txn.getFields().put(fieldName, fieldValue);
            });

    validateRequiredFields(txn.getAmountInAsset(), txn.getFields());

    Sep31Transaction savedTxn = sep31TransactionStore.save(txn);

    return fromTransactionToResponse(savedTxn);
  }

  void validatePatchTransactionFields(Sep31Transaction txn, Sep31PatchTransactionRequest request)
      throws BadRequestException {
    Map<String, AssetInfo.Sep31TxnFieldSpec> expectedFields =
        txn.getRequiredInfoUpdates().getTransaction();
    Map<String, String> fields = request.getFields().getTransaction();
    // validate if any of the fields from the request is not expected in the transaction.
    List<String> unexpectedFields =
        fields.keySet().stream()
            .filter(key -> !expectedFields.containsKey(key))
            .collect(Collectors.toList());

    if (unexpectedFields.size() > 0) {
      throw new BadRequestException(
          String.format("[%s] is not a expected field", unexpectedFields.get(0)));
    }
  }

  void validateAndApplyFeeAndQuote(
      Sep31Transaction txn, Sep31PostTransactionRequest request, JwtToken jwtToken)
      throws AnchorException {
    AssetInfo asset = assetService.getAsset(request.getAssetCode(), request.getAssetIssuer());
    // Check if quote is provided.
    Sep38Quote quote = null;
    if (request.getQuoteId() != null) {
      quote = sep38QuoteStore.findByQuoteId(request.getQuoteId());
      if (quote == null)
        throw new BadRequestException(
            String.format("quote(id=%s) was not a valid quote.", request.getQuoteId()));
    }

    txn.setAmountOutAsset(txn.getAmountInAsset());
    BigDecimal amountIn =
        decimal(txn.getAmountIn()).setScale(asset.getSignificantDecimals(), RoundingMode.HALF_UP);
    GetFeeResponse feeResponse =
        feeIntegration.getFee(
            GetFeeRequest.builder()
                .sendAmount(request.getAmount())
                .sendAsset(request.getAssetCode())
                .receiveAsset(request.getAssetCode())
                .receiveAmount(null)
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .clientDomain(jwtToken.getClientDomain())
                .build());
    BigDecimal amountFee = decimal(feeResponse.getFee().getAmount());
    BigDecimal amountOut = amountIn.subtract(amountFee);
    txn.setAmountOut(String.valueOf(amountOut));

    if (quote != null) {
      // Check quote amounts
      if (!amountEquals(quote.getSellAmount(), txn.getAmountIn())) {
        throw new BadRequestException(
            String.format(
                "Quote amount is [%s] different from the sending amount [%s]",
                quote.getSellAmount(), txn.getAmountIn()));
      }

      // check quote asset
      if (!quote.getSellAsset().equals(txn.getAmountInAsset())) {
        throw new BadRequestException(
            String.format(
                "Quote asset is [%s] different from the transaction sending asset [%s]",
                quote.getSellAmount(), txn.getAmountIn()));
      }
      txn.setQuoteId(request.getQuoteId());
    }
  }

  void validateKyc(String senderId, String receiverId) throws AnchorException {
    Sep12GetCustomerRequest request = Sep12GetCustomerRequest.builder().id(receiverId).build();
    Sep12GetCustomerResponse receiver = this.customerIntegration.getCustomer(request);
    if (receiver == null) {
      throw new Sep31CustomerInfoNeededException("sep31-receiver");
    }

    // TODO: More of sender / receiver customer validation should be implemented in /fee or future
    // /validate-txn API.
    // TODO: Check sender if sender id is not null. This is also related to if we require senderId
  }

  void validateRequiredFields(String assetCode, Map<String, String> fields) throws AnchorException {
    if (fields == null) {
      throw new BadRequestException("'fields' field must have one 'transaction' field");
    }

    AssetResponse fieldSpecs = this.infoResponse.getReceive().get(assetCode);
    Map<String, AssetInfo.Sep31TxnFieldSpec> missingFields =
        fieldSpecs.getFields().getTransaction().entrySet().stream()
            .filter(
                entry -> {
                  AssetInfo.Sep31TxnFieldSpec field = entry.getValue();
                  if (field.isOptional()) return false;
                  return fields.get(entry.getKey()) == null;
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Sep31TxnFieldSpecs sep31MissingTxnFields = new Sep31TxnFieldSpecs();
    sep31MissingTxnFields.setTransaction(missingFields);

    if (missingFields.size() > 0) {
      throw new Sep31MissingFieldException(sep31MissingTxnFields);
    }
  }

  void validateSenderAndReceiver(Sep31PostTransactionRequest request) throws BadRequestException {
    if (request.getReceiverId() == null || request.getSenderId() == null) {
      throw new BadRequestException("receiver_id must be provided.");
    }
    // TODO: We should discuss how to delegate the decision to delegate the senderId and receiverId
    // to the anchor.
  }

  AssetInfo validateAsset(Sep31PostTransactionRequest request) throws BadRequestException {
    String assetCode = request.getAssetCode();
    String assetIssuer = request.getAssetIssuer();
    // Check if the asset is supported in SEP-31
    for (AssetInfo assetInfo : assetService.listAllAssets()) {
      if (assetInfo.getSep31Enabled()
          //          && assetInfo.getIssuer().equals(assetIssuer) TODO: Add this back when
          // demo-wallet sends the issuer.
          && assetInfo.getCode().equals(assetCode)) {
        return assetInfo;
      }
    }
    // the asset is not supported.
    throw new BadRequestException(
        String.format("asset %s:%s is not supported.", assetCode, assetIssuer));
  }

  Sep31GetTransactionResponse fromTransactionToResponse(Sep31Transaction txn) {
    return Sep31GetTransactionResponse.builder()
        .transaction(
            TransactionResponse.builder()
                .id(txn.getId())
                .status(txn.getStatus())
                .statusEta(txn.getStatusEta())
                .amountIn(txn.getAmountIn())
                .amountInAsset(txn.getAmountInAsset())
                .amountOut(txn.getAmountOut())
                .amountOutAsset(txn.getAmountOutAsset())
                .amountFee(txn.getAmountFee())
                .amountFeeAsset(txn.getAmountFeeAsset())
                .stellarAccountId(txn.getStellarAccountId())
                .stellarMemo(txn.getStellarMemo())
                .stellarMemoType(txn.getStellarMemoType())
                .startedAt(txn.getStartedAt())
                .completedAt(txn.getCompletedAt())
                .stellarTransactionId(txn.getStellarTransactionId())
                .externalTransactionId(txn.getExternalTransactionId())
                .refunded(txn.getRefunded())
                // TODO: handle this post-mvp
                // .refunds(txn.getRefunds())
                .requiredInfoMessage(txn.getRequiredInfoMessage())
                .requiredInfoUpdates(txn.getRequiredInfoUpdates())
                .build())
        .build();
  }

  @SneakyThrows
  static Sep31InfoResponse createFromAssets(List<AssetInfo> assetInfos) {
    Sep31InfoResponse response = new Sep31InfoResponse();
    response.setReceive(new HashMap<>());
    for (AssetInfo assetInfo : assetInfos) {
      if (assetInfo.getSep31Enabled()) {
        AssetResponse assetResponse = new AssetResponse();
        assetResponse.setQuotesSupported(assetInfo.getSep31().isQuotesSupported());
        assetResponse.setQuotesRequired(assetInfo.getSep31().isQuotesRequired());
        assetResponse.setFeeFixed(assetInfo.getSend().getFeeFixed());
        assetResponse.setFeePercent(assetInfo.getSend().getFeePercent());
        assetResponse.setMinAmount(assetInfo.getSend().getMinAmount());
        assetResponse.setMaxAmount(assetInfo.getSend().getMaxAmount());
        assetResponse.setFields(assetInfo.getSep31().getFields());
        assetResponse.setSep12(assetInfo.getSep31().getSep12());
        response.getReceive().put(assetInfo.getCode(), assetResponse);
      }
    }

    return response;
  }

  public static class Sep31MissingFieldException extends AnchorException {
    private final Sep31TxnFieldSpecs missingFields;

    public Sep31MissingFieldException(Sep31TxnFieldSpecs missingFields) {
      super();
      this.missingFields = missingFields;
    }

    public Sep31TxnFieldSpecs getMissingFields() {
      return missingFields;
    }
  }

  public static class Sep31CustomerInfoNeededException extends AnchorException {
    private final String type;

    Sep31CustomerInfoNeededException(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }
}
