package org.stellar.anchor.sep31;

import static org.stellar.anchor.api.sep.sep31.Sep31InfoResponse.AssetResponse;
import static org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_SEND;
import static org.stellar.anchor.sep31.Sep31Helper.amountEquals;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.SepHelper.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.SneakyThrows;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.callback.FeeIntegration;
import org.stellar.anchor.api.callback.GetFeeRequest;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.NotFoundException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.AssetInfo.Sep31TxnFieldSpecs;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse;
import org.stellar.anchor.api.sep.sep31.*;
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse.TransactionResponse;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep31Config;
import org.stellar.anchor.event.EventPublishService;
import org.stellar.anchor.event.models.StellarId;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.sep38.Sep38Quote;
import org.stellar.anchor.sep38.Sep38QuoteStore;

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
  }

  public Sep31InfoResponse getInfo() {
    return infoResponse;
  }

  public Sep31PostTransactionResponse postTransaction(
      JwtToken jwtToken, Sep31PostTransactionRequest request) throws AnchorException {
    Context.reset();

    Context.get().setRequest(request);
    Context.get().setJwtToken(jwtToken);

    validateAsset();

    AssetInfo assetInfo = assetService.getAsset(request.getAssetCode(), request.getAssetIssuer());

    // Pre-validation
    validateAmount(request.getAmount());
    validateLanguage(appConfig, request.getLang());
    validateRequiredFields(assetInfo.getCode(), request.getFields().getTransaction());
    validateSenderAndReceiver();
    validateKyc();
    preValidateQuote();

    // Query the fee
    queryFee();

    AssetInfo asset = Context.get().getAsset();
    Sep31Transaction txn =
        new Sep31TransactionBuilder(sep31TransactionStore)
            .id(generateSepTransactionId())
            .status(SepTransactionStatus.PENDING_SENDER.toString())
            .stellarAccountId(asset.getDistributionAccount())
            .amountInAsset(assetInfo.getAssetName())
            .amountIn(request.getAmount())
            .clientDomain(jwtToken.getClientDomain())
            .fields(request.getFields().getTransaction())
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
            .amountIn(new Amount(txn.getAmountIn(), txn.getAmountInAsset()))
            .amountOut(new Amount(txn.getAmountOut(), txn.getAmountOutAsset()))
            .amountFee(new Amount(txn.getAmountFee(), txn.getAmountFeeAsset()))
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
        .stellarAccountId(Context.get().getAsset().getDistributionAccount())
        .stellarMemo(txn.getStellarMemo())
        .stellarMemoType(txn.getStellarMemoType())
        .build();
  }

  void updateAmounts() throws AnchorException {
    Sep31Transaction txn = Context.get().getTransaction();
    Sep31PostTransactionRequest request = Context.get().getRequest();
    Amount feeResponse = Context.get().getFee();
    boolean withoutQuote = request.getQuoteId() == null;

    AssetInfo reqAsset = assetService.getAsset(request.getAssetCode(), request.getAssetIssuer());
    BigDecimal reqAmount = decimal(request.getAmount(), reqAsset);
    BigDecimal fee = decimal(feeResponse.getAmount(), reqAsset);
    String feeAsset = String.valueOf(feeResponse.getAsset());

    BigDecimal amountIn;
    String amountInAsset = reqAsset.getCode();
    BigDecimal amountOut;
    String amountOutAsset;

    if (withoutQuote) {
      boolean strictSend = sep31Config.getPaymentType() == STRICT_SEND;
      // Without quote.
      amountOutAsset = amountInAsset;
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
      // Update transactions
      txn.setAmountFee(String.valueOf(fee));
      txn.setAmountFeeAsset(feeAsset);
    } else {
      // With quote
      Sep38Quote quote = sep38QuoteStore.findByQuoteId(request.getQuoteId());
      txn.setQuoteId(request.getQuoteId());
      amountOutAsset = quote.getBuyAsset();
      amountIn = decimal(quote.getSellAmount(), reqAsset);
      amountOut = decimal(quote.getBuyAmount(), reqAsset);
    }

    // Update transactions
    txn.setAmountIn(String.valueOf(amountIn));
    txn.setAmountInAsset(amountInAsset);
    txn.setAmountOut(String.valueOf(amountOut));
    txn.setAmountOutAsset(amountOutAsset);
  }

  void updateAmounts2() throws AnchorException {
    Sep31Transaction txn = Context.get().getTransaction();
    Sep31PostTransactionRequest request = Context.get().getRequest();
    Amount feeResponse = Context.get().getFee();

    boolean strictSend = sep31Config.getPaymentType() == STRICT_SEND;
    boolean withoutQuote = request.getQuoteId() == null;

    AssetInfo reqAsset = assetService.getAsset(request.getAssetCode(), request.getAssetIssuer());
    BigDecimal reqAmount = decimal(request.getAmount(), reqAsset);
    BigDecimal fee = decimal(feeResponse.getAmount(), reqAsset);
    String feeAsset = String.valueOf(feeResponse.getAsset());

    BigDecimal amountIn;
    String amountInAsset = reqAsset.getAssetName();
    BigDecimal amountOut;
    String amountOutAsset;

    if (withoutQuote) {
      // Without quote.
      amountOutAsset = amountInAsset;
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
    } else {
      // With quote
      Sep38Quote quote = sep38QuoteStore.findByQuoteId(request.getQuoteId());
      txn.setQuoteId(request.getQuoteId());
      amountOutAsset = quote.getBuyAsset();
      BigDecimal buyAmount = decimal(quote.getBuyAmount(), reqAsset);
      BigDecimal sellAmount = decimal(quote.getSellAmount(), reqAsset);

      if (feeResponse.getAsset().equals(quote.getSellAsset())) {
        // Fee is applied before the quote
        if (strictSend) {
          // amount_in=req.amount
          // amount_out=quote.buy_amount
          amountIn = reqAmount;
          amountOut = buyAmount;
          // Validate:
          // TODO - should this check be ((amount_in - fee) / quote.price) == quote.buy_amount?
          // for simplicity, i think quote.sell_amount = amount_in
          //          if (sellAmount.compareTo(amountIn.subtract(fee)) != 0) {
          //            throw new BadRequestException(
          //                "Bad quote. quote.sell_amount does not equal to (amount_in -
          // amount_fee)");
          //          }
        } else {
          // amount_in = req.amount + fee
          // amount_out = quote.buy_amount
          amountIn = reqAmount.add(fee);
          amountOut = buyAmount;
          // Validate:
          //   quote.sell_amount = req.amount
          if (sellAmount.compareTo(reqAmount) != 0) {
            throw new BadRequestException(
                "Bad quote. quote.sell_amount does not equal to (amount_in - amount_fee)");
          }
        }
      } else {
        // Fee is applied after the quote
        if (strictSend) {
          // amount_in=req.amount
          // amount_out=quote.buy_amount-fee
          amountIn = reqAmount;
          amountOut = buyAmount.subtract(fee);
          // Validate:
          //   quote.sell_amount = req.amount
          if (sellAmount.compareTo(reqAmount) != 0) {
            throw new BadRequestException(
                "Bad quote. quote.sell_amount does not equal to amount_in");
          }
        } else {
          // amount_in=sellAmount
          // amount_out=quote.buy_amount -fee
          amountIn = sellAmount;
          amountOut = buyAmount.subtract(fee);
        }
        if (amountIn.compareTo(sellAmount) != 0) {
          throw new BadRequestException("Bad quote. amount_in does not equal to request amount.");
        }
      }
    }

    // Update transactions
    txn.setAmountFee(String.valueOf(fee));
    txn.setAmountFeeAsset(feeAsset);
    txn.setAmountIn(String.valueOf(amountIn));
    txn.setAmountInAsset(amountInAsset);
    txn.setAmountOut(String.valueOf(amountOut));
    txn.setAmountOutAsset(amountOutAsset);
  }

  private void updateDepositInfo(Sep31Transaction txn) {
    Sep31DepositInfo depositInfo = sep31DepositInfoGenerator.getSep31DepositInfo(txn);
    txn.setStellarAccountId(depositInfo.getStellarAddress());
    txn.setStellarMemo(depositInfo.getMemo());
    txn.setStellarMemoType(depositInfo.getMemoType());
  }

  public Sep31GetTransactionResponse getTransaction(String id) throws AnchorException {
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
    Context.reset();

    Sep31Transaction txn = sep31TransactionStore.findByTransactionId(request.getId());
    Context.get().setTransaction(txn);

    if (txn == null) {
      throw new NotFoundException(String.format("transaction (id=%s) not found", request.getId()));
    }

    // validate if the transaction is in the pending_transaction_info_update status
    if (!Objects.equals(
        txn.getStatus(), SepTransactionStatus.PENDING_TRANSACTION_INFO_UPDATE.toString())) {
      throw new BadRequestException(
          String.format("transaction (id=%s) does not need update", txn.getId()));
    }

    validatePatchTransactionFields(txn, request);

    request
        .getFields()
        .getTransaction()
        .forEach((fieldName, fieldValue) -> txn.getFields().put(fieldName, fieldValue));

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

  void preValidateQuote() throws AnchorException {
    Sep31PostTransactionRequest request = Context.get().getRequest();

    // Check if quote is provided.
    if (request.getQuoteId() != null) {
      Sep38Quote quote = sep38QuoteStore.findByQuoteId(request.getQuoteId());
      if (quote == null)
        throw new BadRequestException(
            String.format("quote(id=%s) was not a valid quote.", request.getQuoteId()));
      // Check quote amounts
      if (!amountEquals(quote.getSellAmount(), request.getAmount())) {
        throw new BadRequestException(
            String.format(
                "Quote amount is [%s] different from the sending amount [%s]",
                quote.getSellAmount(), request.getAmount()));
      }
      String assetName =
          assetService.getAsset(request.getAssetCode(), request.getAssetIssuer()).getAssetName();
      // check quote asset
      if (!quote.getSellAsset().equals(assetName)) {
        throw new BadRequestException(
            String.format(
                "Quote asset is [%s] different from the transaction sending asset [%s]",
                quote.getSellAsset(), assetName));
      }
    }
  }

  void queryFee() throws AnchorException {
    Sep31PostTransactionRequest request = Context.get().getRequest();
    JwtToken token = Context.get().getJwtToken();
    String assetName =
        assetService.getAsset(request.getAssetCode(), request.getAssetIssuer()).getAssetName();
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
                    .clientDomain(token.getClientDomain())
                    .build())
            .getFee();

    Context.get().setFee(fee);
  }

  void validateKyc() throws AnchorException {
    String receiverId = Context.get().getRequest().getReceiverId();
    Sep12GetCustomerRequest request = Sep12GetCustomerRequest.builder().id(receiverId).build();
    Sep12GetCustomerResponse receiver = this.customerIntegration.getCustomer(request);
    if (receiver == null) {
      throw new Sep31CustomerInfoNeededException("sep31-receiver");
    }

    // TODO: More of sender / receiver customer validation should be implemented in /fee or future
    // /validate-txn API.
    // TODO: Check sender if sender id is not null. This is also related to if we require senderId
  }

  void validateSenderAndReceiver() throws BadRequestException {
    Sep31PostTransactionRequest request = Context.get().getRequest();
    if (request.getReceiverId() == null || request.getSenderId() == null) {
      throw new BadRequestException("receiver_id must be provided.");
    }
    // TODO: We should discuss how to delegate the decision to delegate the senderId and receiverId
    // to the anchor.
  }

  void validateAsset() throws BadRequestException {
    Sep31PostTransactionRequest request = Context.get().getRequest();
    String assetCode = request.getAssetCode();
    String assetIssuer = request.getAssetIssuer();
    // Check if the asset is supported in SEP-31
    for (AssetInfo asset : assetService.listAllAssets()) {
      if (asset.getSep31Enabled()
          //          && assetInfo.getIssuer().equals(assetIssuer) TODO: Add this back when
          // demo-wallet sends the issuer.
          && asset.getCode().equals(assetCode)) {
        Context.get().setAsset(asset);
        return;
      }
    }
    // the asset is not supported.
    throw new BadRequestException(
        String.format("asset %s:%s is not supported.", assetCode, assetIssuer));
  }

  void validateRequiredFields(String assetCode, Map<String, String> fields) throws AnchorException {
    if (fields == null) {
      throw new BadRequestException("'fields' field must have one 'transaction' field");
    }

    if (assetCode == null) {
      throw new BadRequestException("Missing asset code.");
    }

    AssetResponse fieldSpecs = this.infoResponse.getReceive().get(assetCode);
    if (fieldSpecs == null) {
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

  @Data
  public static class Context {
    Sep31Transaction transaction;
    Sep31PostTransactionRequest request;
    JwtToken jwtToken;
    Amount fee;
    AssetInfo asset;
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
