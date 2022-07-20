package org.stellar.anchor.sep31;

import static org.stellar.anchor.api.sep.sep31.Sep31InfoResponse.AssetResponse;
import static org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_SEND;
import static org.stellar.anchor.util.Log.*;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.formatAmount;
import static org.stellar.anchor.util.SepHelper.*;
import static org.stellar.anchor.util.SepLanguageHelper.validateLanguage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.SneakyThrows;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.callback.FeeIntegration;
import org.stellar.anchor.api.callback.GetFeeRequest;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.AssetInfo.Sep12Operation;
import org.stellar.anchor.api.sep.AssetInfo.Sep31TxnFieldSpecs;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse;
import org.stellar.anchor.api.sep.sep31.*;
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse.TransactionResponse;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep31Config;
import org.stellar.anchor.event.EventPublishService;
import org.stellar.anchor.event.models.StellarId;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.sep38.Sep38Quote;
import org.stellar.anchor.sep38.Sep38QuoteStore;
import org.stellar.anchor.util.Log;

public class Sep31Service {
  private final AppConfig appConfig;
  private final Sep31Config sep31Config;
  private final Sep31TransactionStore sep31TransactionStore;
  private final Sep31DepositInfoGenerator sep31DepositInfoGenerator;
  private final Sep38QuoteStore sep38QuoteStore;
  private final AssetService assetService;
  private final FeeIntegration feeIntegration;
  private final CustomerIntegration customerIntegration;
  private final Sep31InfoResponse infoResponse;
  private final EventPublishService eventService;

  public Sep31Service(
      AppConfig appConfig,
      Sep31Config sep31Config,
      Sep31TransactionStore sep31TransactionStore,
      Sep31DepositInfoGenerator sep31DepositInfoGenerator,
      Sep38QuoteStore sep38QuoteStore,
      AssetService assetService,
      FeeIntegration feeIntegration,
      CustomerIntegration customerIntegration,
      EventPublishService eventService) {
    debug("appConfig:", appConfig);
    debug("sep31Config:", sep31Config);
    this.appConfig = appConfig;
    this.sep31Config = sep31Config;
    this.sep31TransactionStore = sep31TransactionStore;
    this.sep31DepositInfoGenerator = sep31DepositInfoGenerator;
    this.sep38QuoteStore = sep38QuoteStore;
    this.assetService = assetService;
    this.feeIntegration = feeIntegration;
    this.customerIntegration = customerIntegration;
    this.eventService = eventService;
    this.infoResponse = createFromAssets(assetService.listAllAssets());
    Log.info("Sep31Service initialized.");
  }

  public Sep31InfoResponse getInfo() {
    return infoResponse;
  }

  public Sep31PostTransactionResponse postTransaction(
      JwtToken jwtToken, Sep31PostTransactionRequest request) throws AnchorException {
    Context.reset();
    Context.get().setRequest(request);
    Context.get().setJwtToken(jwtToken);

    AssetInfo assetInfo = assetService.getAsset(request.getAssetCode(), request.getAssetIssuer());
    if (assetInfo == null) {
      // the asset is not supported.
      infoF("Asset: [{}:{}]", request.getAssetCode(), request.getAssetIssuer());
      throw new BadRequestException(
          String.format(
              "asset %s:%s is not supported.", request.getAssetCode(), request.getAssetIssuer()));
    }

    // Pre-validation
    validateAmount(request.getAmount());
    validateLanguage(appConfig, request.getLang());
    if (request.getFields() == null) {
      infoF(
          "POST /transaction with id ({}) cannot have empty `fields`", jwtToken.getTransactionId());
      throw new BadRequestException("'fields' field cannot be empty");
    }

    Context.get().setAsset(assetInfo);
    Context.get().setTransactionFields(request.getFields().getTransaction());

    validateRequiredFields();
    validateSenderAndReceiver();
    preValidateQuote();

    // Query the fee
    updateFee();

    // Get the creator's stellarId
    StellarId creatorStellarId =
        StellarId.builder()
            .account(Objects.requireNonNullElse(jwtToken.getMuxedAccount(), jwtToken.getAccount()))
            .memo(jwtToken.getAccountMemo())
            .memoType(jwtToken.getAccountMemo() == null ? null : "id")
            .build();

    AssetInfo asset = Context.get().getAsset();
    Amount fee = Context.get().getFee();
    Sep31Transaction txn =
        new Sep31TransactionBuilder(sep31TransactionStore)
            .id(generateSepTransactionId())
            .status(SepTransactionStatus.PENDING_SENDER.toString())
            .statusEta(null)
            .amountFee(fee.getAmount())
            .amountFeeAsset(fee.getAsset())
            .startedAt(Instant.now())
            .completedAt(null)
            .stellarTransactionId(null)
            .externalTransactionId(null)
            .requiredInfoMessage(null)
            .quoteId(request.getQuoteId())
            .clientDomain(jwtToken.getClientDomain())
            .requiredInfoUpdates(null)
            .fields(request.getFields().getTransaction())
            .refunded(null)
            .refunds(null)
            .senderId(Context.get().getRequest().getSenderId())
            .receiverId(Context.get().getRequest().getReceiverId())
            .creator(creatorStellarId)
            // updateAmounts will update these ⬇️
            .amountIn(request.getAmount())
            .amountInAsset(assetInfo.getAssetName())
            .amountOut(null)
            .amountOutAsset(null)
            // updateDepositInfo will update these ⬇️
            .stellarAccountId(asset.getDistributionAccount())
            .stellarMemo(null)
            .stellarMemoType(null)
            .build();

    Context.get().setTransaction(txn);
    updateAmounts();
    updateDepositInfo(txn);
    sep31TransactionStore.save(txn);

    TransactionEvent event =
        TransactionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .type(TransactionEvent.Type.TRANSACTION_CREATED)
            .id(txn.getId())
            .sep(TransactionEvent.Sep.SEP_31)
            .kind(TransactionEvent.Kind.RECEIVE)
            .status(TransactionEvent.Status.PENDING_SENDER)
            .statusChange(
                new TransactionEvent.StatusChange(null, TransactionEvent.Status.PENDING_SENDER))
            .amountExpected(new Amount(txn.getAmountIn(), txn.getAmountInAsset()))
            .amountIn(new Amount(txn.getAmountIn(), txn.getAmountInAsset()))
            .amountOut(new Amount(txn.getAmountOut(), txn.getAmountOutAsset()))
            .amountFee(new Amount(txn.getAmountFee(), txn.getAmountFeeAsset()))
            .quoteId(txn.getQuoteId())
            .startedAt(txn.getStartedAt())
            .updatedAt(txn.getStartedAt())
            .completedAt(null)
            .transferReceivedAt(null)
            .message(null)
            .refunds(null)
            .stellarTransactions(null)
            .externalTransactionId(null)
            .custodialTransactionId(null)
            .sourceAccount(request.getSenderId())
            .destinationAccount(request.getReceiverId())
            .creator(creatorStellarId)
            .build();
    eventService.publish(event);

    return Sep31PostTransactionResponse.builder()
        .id(txn.getId())
        .stellarAccountId(txn.getStellarAccountId())
        .stellarMemo(txn.getStellarMemo())
        .stellarMemoType(txn.getStellarMemoType())
        .build();
  }

  void updateAmounts() throws AnchorException {
    Sep31PostTransactionRequest request = Context.get().getRequest();
    if (request.getQuoteId() != null) {
      updateTxAmountsBasedOnQuote();
      return;
    }
    updateTxAmountsWhenNoQuoteWasUsed();
  }

  void updateTxAmountsBasedOnQuote() throws AnchorException {
    Sep38Quote quote = Context.get().getQuote();
    if (quote == null) {
      infoF("Quote for transaction ({}) not found", Context.get().getTransaction().getId());
      throw new ServerErrorException("Quote not found.");
    }

    Sep31Transaction txn = Context.get().getTransaction();
    debugF("Updating transaction ({}) with quote ({})", txn.getId(), quote.getId());
    txn.setAmountInAsset(quote.getSellAsset());
    txn.setAmountIn(quote.getSellAmount());
    txn.setAmountOutAsset(quote.getBuyAsset());
    txn.setAmountOut(quote.getBuyAmount());
  }

  void updateTxAmountsWhenNoQuoteWasUsed() {
    Sep31PostTransactionRequest request = Context.get().getRequest();
    Sep31Transaction txn = Context.get().getTransaction();
    Amount feeResponse = Context.get().getFee();

    AssetInfo reqAsset = Context.get().getAsset();
    int scale = reqAsset.getSignificantDecimals();
    BigDecimal reqAmount = decimal(request.getAmount(), scale);
    BigDecimal fee = decimal(feeResponse.getAmount(), scale);

    BigDecimal amountIn;
    BigDecimal amountOut;
    boolean strictSend = sep31Config.getPaymentType() == STRICT_SEND;
    if (strictSend) {
      // amount_in = req.amount
      // amount_out = amount_in - amount fee
      amountIn = reqAmount;
      amountOut = amountIn.subtract(fee);
    } else {
      // amount_in = req.amount + fee
      // amount_out = req.amount
      amountIn = reqAmount.add(fee);
      amountOut = reqAmount;
    }
    debugF("Updating transaction ({}) with fee ({}) - reqAsset ({})", txn.getId(), fee, reqAsset);

    // Update transaction
    txn.setAmountIn(formatAmount(amountIn, scale));
    txn.setAmountInAsset(reqAsset.getAssetName());
    txn.setAmountOut(formatAmount(amountOut, scale));
    txn.setAmountOutAsset(reqAsset.getAssetName());

    // Update fee
    String feeStr = formatAmount(fee, scale);
    txn.setAmountFee(feeStr);
    Context.get().getFee().setAmount(feeStr);
  }

  private void updateDepositInfo(Sep31Transaction txn) {
    Sep31DepositInfo depositInfo = sep31DepositInfoGenerator.getSep31DepositInfo(txn);
    infoF("Updating transaction ({}) with depositInfo ({})", txn.getId(), depositInfo);
    txn.setStellarAccountId(depositInfo.getStellarAddress());
    txn.setStellarMemo(depositInfo.getMemo());
    txn.setStellarMemoType(depositInfo.getMemoType());
  }

  public Sep31GetTransactionResponse getTransaction(String id) throws AnchorException {
    if (id == null) {
      info("Unable to get transaction, 'id' is not provided");
      throw new BadRequestException("'id' is not provided");
    }
    Sep31Transaction txn = sep31TransactionStore.findByTransactionId(id);
    if (txn == null) {
      infoF("Transaction ({}) not found", id);
      throw new NotFoundException(String.format("transaction (id=%s) not found", id));
    }

    return fromTransactionToResponse(txn);
  }

  public Sep31GetTransactionResponse patchTransaction(Sep31PatchTransactionRequest request)
      throws AnchorException {
    Context.reset();

    Sep31Transaction txn = sep31TransactionStore.findByTransactionId(request.getId());
    Context.get().setTransaction(txn);

    if (txn == null) {
      infoF("Transaction ({}) not found", request.getId());
      throw new NotFoundException(String.format("transaction (id=%s) not found", request.getId()));
    }

    // validate if the transaction is in the pending_transaction_info_update status
    if (!Objects.equals(
        txn.getStatus(), SepTransactionStatus.PENDING_TRANSACTION_INFO_UPDATE.toString())) {
      infoF("Transaction ({}) does not need update", txn.getId());
      throw new BadRequestException(
          String.format("transaction (id=%s) does not need update", txn.getId()));
    }

    validatePatchTransactionFields(txn, request);

    request
        .getFields()
        .getTransaction()
        .forEach((fieldName, fieldValue) -> txn.getFields().put(fieldName, fieldValue));

    AssetInfo assetInfo = assetService.getAsset(txn.getAmountInAsset());
    Context.get().setAsset(assetInfo);
    Context.get().setTransactionFields(txn.getFields());
    validateRequiredFields();

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
      infoF("Transaction ({}) has an unexpected field", txn.getId(), unexpectedFields.get(0));
      throw new BadRequestException(
          String.format("[%s] is not a expected field", unexpectedFields.get(0)));
    }
  }

  void preValidateQuote() throws AnchorException {
    Sep31PostTransactionRequest request = Context.get().getRequest();
    AssetInfo assetInfo = Context.get().getAsset();
    boolean isQuotesRequired = assetInfo.getSep31().isQuotesRequired();
    boolean isQuotesSupported = assetInfo.getSep31().isQuotesSupported();

    if (isQuotesRequired && request.getQuoteId() == null) {
      throw new BadRequestException("quotes_required is set to true; quote id cannot be empty");
    }

    // Check if quote is provided.
    if (!isQuotesSupported || request.getQuoteId() == null) {
      return;
    }

    Sep38Quote quote = sep38QuoteStore.findByQuoteId(request.getQuoteId());
    if (quote == null) {
      infoF("Quote ({}) was not found", request.getQuoteId());
      throw new BadRequestException(
          String.format("quote(id=%s) was not found.", request.getQuoteId()));
    }

    // Check quote amounts: `post_transaction.amount == quote.sell_amount`
    if (!amountEquals(request.getAmount(), quote.getSellAmount())) {
      infoF(
          "Quote ({}) - sellAmount ({}) is different from the SEP-31 transaction amount ({})",
          request.getQuoteId(),
          quote.getSellAmount(),
          request.getAmount());
      throw new BadRequestException(
          String.format(
              "Quote sell amount [%s] is different from the SEP-31 transaction amount [%s]",
              quote.getSellAmount(), request.getAmount()));
    }

    // Check quote asset: `post_transaction.asset == quote.sell_asset`
    String assetName =
        assetService.getAsset(request.getAssetCode(), request.getAssetIssuer()).getAssetName();
    if (!assetName.equals(quote.getSellAsset())) {
      infoF(
          "Quote ({}) - sellAsset ({}) is different from the SEP-31 transaction asset ({})",
          request.getQuoteId(),
          quote.getSellAsset(),
          assetName);
      throw new BadRequestException(
          String.format(
              "Quote sell asset [%s] is different from the SEP-31 transaction asset [%s]",
              quote.getSellAsset(), assetName));
    }

    Context.get().setQuote(quote);
  }

  void updateFee() throws AnchorException {
    Sep38Quote quote = Context.get().getQuote();
    if (quote != null) {
      if (quote.getFee() == null) {
        infoF("Quote: ({}) is missing the 'fee' field", quote.getId());
        throw new SepValidationException("Quote is missing the 'fee' field");
      }
      Amount fee = new Amount(quote.getFee().getTotal(), quote.getFee().getAsset());
      Context.get().setFee(fee);
      return;
    }

    Sep31PostTransactionRequest request = Context.get().getRequest();
    JwtToken token = Context.get().getJwtToken();
    String assetName =
        assetService.getAsset(request.getAssetCode(), request.getAssetIssuer()).getAssetName();
    infoF("Requesting fee for request ({})", request);
    Amount fee =
        feeIntegration
            .getFee(
                GetFeeRequest.builder()
                    .sendAmount(request.getAmount())
                    .sendAsset(assetName)
                    .receiveAsset(
                        (request.getDestinationAsset() == null)
                            ? assetName
                            : request.getDestinationAsset())
                    .receiveAmount(null)
                    .senderId(request.getSenderId())
                    .receiverId(request.getReceiverId())
                    .clientId(token.getAccount())
                    .build())
            .getFee();
    infoF("Fee for request ({}) is ({})", request, fee);
    Context.get().setFee(fee);
  }

  void validateSenderAndReceiver() throws AnchorException {
    String receiverId = Context.get().getRequest().getReceiverId();
    if (receiverId == null) {
      infoF("'receiver_id' cannot be empty for request ({})", Context.get().getRequest());
      throw new BadRequestException("receiver_id cannot be empty.");
    }

    // TODO: Populate customer type with the first receiver type; this is a temporary fix for cases
    // where customerType is required
    Sep12Operation sep12Operation = Context.get().getAsset().getSep31().getSep12();
    String receiverType = null;
    if (sep12Operation != null) {
      Optional<String> receiverTypeOptional =
          sep12Operation.getReceiver().getTypes().keySet().stream().findFirst();
      receiverType = receiverTypeOptional.isPresent() ? receiverTypeOptional.get() : null;
    }
    Sep12GetCustomerRequest request =
        Sep12GetCustomerRequest.builder().id(receiverId).type(receiverType).build();
    Sep12GetCustomerResponse receiver = this.customerIntegration.getCustomer(request);
    if (receiver == null) {
      infoF("Customer (receiver) info needed for request ({})", Context.get().getRequest());
      throw new Sep31CustomerInfoNeededException("sep31-receiver");
    }

    String senderId = Context.get().getRequest().getSenderId();
    if (senderId == null) {
      infoF("'sender_id' cannot be empty for request ({})", Context.get().getRequest());
      throw new BadRequestException("sender_id cannot be empty.");
    }

    // TODO: Populate customer type with the first sender type; this is a temporary fix for cases
    // where customerType is required
    String senderType = null;
    if (sep12Operation != null) {
      Optional<String> senderTypeOptional =
          Context.get().getAsset().getSep31().getSep12().getSender().getTypes().keySet().stream()
              .findFirst();
      senderType = senderTypeOptional.isPresent() ? senderTypeOptional.get() : null;
    }
    request = Sep12GetCustomerRequest.builder().id(senderId).type(senderType).build();
    Sep12GetCustomerResponse sender = this.customerIntegration.getCustomer(request);
    if (sender == null) {
      infoF("Customer (sender) info needed for request ({})", Context.get().getRequest());
      throw new Sep31CustomerInfoNeededException("sep31-sender");
    }
  }

  void validateRequiredFields() throws AnchorException {
    String assetCode = Context.get().getAsset().getCode();
    Map<String, String> fields = Context.get().getTransactionFields();
    if (fields == null) {
      infoF(
          "'fields' field must have one 'transaction' field for request ({})",
          Context.get().getRequest());
      throw new BadRequestException("'fields' field must have one 'transaction' field");
    }

    if (assetCode == null) {
      infoF("Missing asset code for request ({})", Context.get().getRequest());
      throw new BadRequestException("Missing asset code.");
    }

    AssetResponse fieldSpecs = this.infoResponse.getReceive().get(assetCode);
    if (fieldSpecs == null) {
      infoF("Asset not found for request ({})", Context.get().getRequest());
      throw new SepNotFoundException("Asset not found.");
    }

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
      infoF(
          "Missing SEP-31 fields ({}) for request ({})",
          sep31MissingTxnFields,
          Context.get().getRequest());
      throw new Sep31MissingFieldException(sep31MissingTxnFields);
    }
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
        boolean isQuotesSupported = assetInfo.getSep31().isQuotesSupported();
        boolean isQuotesRequired = assetInfo.getSep31().isQuotesRequired();
        if (isQuotesRequired == true && isQuotesSupported == false) {
          throw new SepValidationException(
              "if quotes_required is true, quotes_supported must also be true");
        }
        AssetResponse assetResponse = new AssetResponse();
        assetResponse.setQuotesSupported(isQuotesSupported);
        assetResponse.setQuotesRequired(isQuotesRequired);
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

  @Data
  public static class Context {
    Sep31Transaction transaction;
    Sep31PostTransactionRequest request;
    Sep38Quote quote;
    JwtToken jwtToken;
    Amount fee;
    AssetInfo asset;
    Map<String, String> transactionFields;
    static ThreadLocal<Context> context = new ThreadLocal<>();

    public static Context get() {
      if (context.get() == null) {
        context.set(new Context());
      }
      return context.get();
    }

    public static void reset() {
      context.set(null);
    }
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
