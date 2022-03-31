package org.stellar.anchor.sep31;

import lombok.SneakyThrows;
import org.stellar.anchor.asset.AssetInfo;
import org.stellar.anchor.asset.AssetInfo.Sep31TxnFields;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep31Config;
import org.stellar.anchor.dto.sep12.Sep12GetCustomerRequest;
import org.stellar.anchor.dto.sep12.Sep12GetCustomerResponse;
import org.stellar.anchor.dto.sep31.*;
import org.stellar.anchor.dto.sep31.Sep31GetTransactionResponse.TransactionResponse;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.exception.BadRequestException;
import org.stellar.anchor.exception.NotFoundException;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import org.stellar.anchor.integration.fee.FeeIntegration;
import org.stellar.anchor.integration.rate.RateIntegration;
import org.stellar.anchor.model.Sep31Transaction;
import org.stellar.anchor.model.Sep31TransactionBuilder;
import org.stellar.anchor.model.Sep38Quote;
import org.stellar.anchor.model.TransactionStatus;
import org.stellar.anchor.paymentservice.Payment;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.sep38.Sep38QuoteStore;
import org.stellar.platform.apis.callbacks.requests.GetFeeRequest;
import org.stellar.platform.apis.callbacks.responses.GetFeeResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.stellar.anchor.dto.sep31.Sep31InfoResponse.AssetResponse;
import static org.stellar.anchor.util.SepHelper.*;

public class Sep31Service {
  private final AppConfig appConfig;
  private final Sep31Config sep31Config;
  private final Sep31TransactionStore sep31TransactionStore;
  private final Sep38QuoteStore sep38QuoteStore;
  private final AssetService assetService;
  private FeeIntegration feeIntegration;
  private final CustomerIntegration customerIntegration;
  private Sep31InfoResponse infoResponse;

  @SneakyThrows
  public Sep31Service(
      AppConfig appConfig,
      Sep31Config sep31Config,
      Sep31TransactionStore sep31TransactionStore,
      Sep38QuoteStore sep38QuoteStore,
      AssetService assetService,
      FeeIntegration feeIntegration,
      CustomerIntegration customerIntegration) {
    this.appConfig = appConfig;
    this.sep31Config = sep31Config;
    this.sep31TransactionStore = sep31TransactionStore;
    this.sep38QuoteStore = sep38QuoteStore;
    this.assetService = assetService;
    this.feeIntegration = feeIntegration;
    this.customerIntegration = customerIntegration;
    this.infoResponse = createFromAsset(assetService.listAllAssets());
  }

  public Sep31InfoResponse getInfo() {
    return infoResponse;
  }

  public Sep31PostTransactionResponse postTransaction(
      JwtToken jwtToken, Sep31PostTransactionRequest request) throws AnchorException {

    validateAsset(request.getAssetCode(), request.getAssetIssuer());
    validateAmount("", request.getAmount());
    validateLanguage(appConfig, request.getLang());
    validateFields(request.getFields());
    validateSenderAndReceiver(request.getSenderId(), request.getReceiverId());
    validateFields(request.getFields(), request.getAssetCode());

    Sep31Transaction txn =
        new Sep31TransactionBuilder(sep31TransactionStore)
            .id(generateTransactionId())
            .status(TransactionStatus.PENDING_SENDER.toString())
            .stellarAccountId(jwtToken.getAccount())
            .amountInAsset(request.getAssetCode())
            .amountIn(request.getAmount())
            .clientDomain(jwtToken.getClientDomain())
            .build();

    validateAndUpdateKyc(request.getSenderId(), request.getReceiverId());
    validateAndApplyFeeAndQuote(txn, request, jwtToken);

    sep31TransactionStore.save(txn);

    return Sep31PostTransactionResponse.builder()
        .id(txn.getId())
        .stellarAccountId(txn.getStellarAccountId())
        .stellarMemo(txn.getStellarMemo())
        .stellarMemo(txn.getStellarMemoType())
        .build();
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

  public void patchTransaction(Sep31PatchTransactionRequest request) throws SepException, AnchorException {
    Sep31Transaction txn = sep31TransactionStore.findByTransactionId(request.getId());
    if (txn==null) {
      throw new NotFoundException(String.format("transaction (id=%s) not found", request.getId()));
    }

    if (!Objects.equals(txn.getStatus(), TransactionStatus.PENDING_TRANSACTION_INFO_UPDATE.toString())) {
      throw new BadRequestException(String.format("transaction (id=%s) does not need update", txn.getId()));
    }

    validatePatchTransactionFields(txn, request);

    return;
  }

  void validatePatchTransactionFields(Sep31Transaction txn, Sep31PatchTransactionRequest request) throws BadRequestException {
    Sep31TxnFields expectedFields = txn.getRequiredInfoUpdates();
    Sep31TxnFields fields = request.getFields();
    List<String> badFields = fields.getTransaction().keySet().stream().filter(key ->
       !expectedFields.getTransaction().containsKey(key)
    ).collect(Collectors.toList());

    if (badFields.size() > 0) {
      throw new BadRequestException(String.format("[%s] is not a expected field", badFields.get(0)));
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

    if (quote == null) {
      txn.setAmountOutAsset(txn.getAmountInAsset());
      BigDecimal amountIn =
          new BigDecimal(txn.getAmountIn())
              .setScale(asset.getSignificantDecimals(), RoundingMode.HALF_UP);
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
      BigDecimal amountFee = new BigDecimal(feeResponse.getFee().getAmount());
      BigDecimal amountOut = amountIn.subtract(amountFee);
      txn.setAmountOut(String.valueOf(amountOut));
    } else {
      txn.setQuoteId(request.getQuoteId());
    }
  }

  void validateAndUpdateKyc(String senderId, String receiverId) throws AnchorException {
    Sep12GetCustomerRequest request = Sep12GetCustomerRequest.builder().id(receiverId).build();
    Sep12GetCustomerResponse receiver = this.customerIntegration.getCustomer(request);
    if (receiver == null) {
      throw new Sep31CustomerInfoNeededException("sep31-receiver");
    }

    // TODO: Check receiver types
    // TODO: Update receiver fields in KYC
    // TODO: Check sender.
  }

  void validateFields(Sep31TxnFields fields, String assetCode) throws Sep31MissingFieldException {
    AssetResponse assetFields = this.infoResponse.getReceive().get(assetCode);
    Map<String, AssetInfo.Sep31TxnField> missingFields =
        assetFields.getFields().getTransaction().entrySet().stream()
            .filter(
                entry -> {
                  AssetInfo.Sep31TxnField field = entry.getValue();
                  if (field.isOptional()) return false;
                  return fields.getTransaction().get(entry.getKey()) == null;
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Sep31TxnFields sep31MissingTxnFields = new Sep31TxnFields();
    sep31MissingTxnFields.setTransaction(missingFields);

    if (missingFields.size() > 0) {
      throw new Sep31MissingFieldException(sep31MissingTxnFields);
    }
  }

  void validateSenderAndReceiver(String senderId, String receiverId) throws BadRequestException {
    if (senderId == null || receiverId == null) {
      throw new BadRequestException("sender_id and receiver_id must be provided.");
    }
  }

  void validateFields(Sep31TxnFields fields) throws BadRequestException {
    if (fields.getTransaction() == null) {
      throw new BadRequestException("'fields' field must have one 'transaction' field");
    }
  }

  void validateAsset(String assetCode, String assetIssuer) throws BadRequestException {
    // Check if the asset is supported in SEP-31
    if (assetService.listAllAssets().stream()
        .noneMatch(
            assetInfo ->
                assetInfo.getSep31Enabled()
                    && assetInfo.getIssuer().equals(assetIssuer)
                    && assetInfo.getCode().equals(assetCode))) {
      // the asset is not supported.
      throw new BadRequestException(
          String.format("asset %s:%s is not supported.", assetCode, assetIssuer));
    }
  }

  @SneakyThrows
  static Sep31InfoResponse createFromAsset(List<AssetInfo> assetInfos) {
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
    private final Sep31TxnFields missingFields;

    public Sep31MissingFieldException(Sep31TxnFields missingFields) {
      super();
      this.missingFields = missingFields;
    }

    public Sep31TxnFields getMissingFields() {
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
