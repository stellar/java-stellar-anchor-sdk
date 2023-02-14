package org.stellar.anchor.sep31;

import static org.stellar.anchor.api.sep.sep31.Sep31InfoResponse.AssetResponse;
import static org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_SEND;
import static org.stellar.anchor.util.Log.*;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.formatAmount;
import static org.stellar.anchor.util.SepHelper.*;
import static org.stellar.anchor.util.SepLanguageHelper.validateLanguage;
import static org.stellar.anchor.util.StringHelper.isEmpty;
import static org.stellar.sdk.xdr.MemoType.MEMO_NONE;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
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
import org.stellar.anchor.api.sep.sep12.Sep12Status;
import org.stellar.anchor.api.sep.sep31.*;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.Customers;
import org.stellar.anchor.api.shared.StellarId;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep31Config;
import org.stellar.anchor.event.EventPublishService;
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
    this.infoResponse = sep31InfoResponseFromAssetInfoList(assetService.listAllAssets());
    Log.info("Sep31Service initialized.");
  }

  public Sep31InfoResponse getInfo() {
    return infoResponse;
  }

  @Transactional(rollbackOn = {AnchorException.class, RuntimeException.class})
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
    Context.get().setAsset(assetInfo);

    // Pre-validation
    validateAmount(request.getAmount());
    validateAmountLimit(
        "sell_",
        request.getAmount(),
        assetInfo.getSend().getMinAmount(),
        assetInfo.getSend().getMaxAmount());
    validateLanguage(appConfig, request.getLang());

    /*
     * TODO:
     *  - conclude if we can drop the usage of `fields`.
     * TODO: if we can't stop using fields, we should:
     *  - check if `fields` are needed. If not, ignore this part of the code
     *  - make sure fields are not getting stored in the database
     *  - make sure fields are being forwarded in the TransactionEvent
     */
    if (request.getFields() == null) {
      infoF(
          "POST /transaction with id ({}) cannot have empty `fields`", jwtToken.getTransactionId());
      throw new BadRequestException("'fields' field cannot be empty");
    }
    Context.get().setTransactionFields(request.getFields().getTransaction());
    validateRequiredFields();

    // Validation that execute HTTP requests
    validateSenderAndReceiver();
    preValidateQuote();

    // Query the fee
    updateFee();

    // Get the creator's stellarId
    StellarId creatorStellarId =
        StellarId.builder()
            .account(Objects.requireNonNullElse(jwtToken.getMuxedAccount(), jwtToken.getAccount()))
            .build();

    Amount fee = Context.get().getFee();
    Instant now = Instant.now();
    Sep31Transaction txn =
        new Sep31TransactionBuilder(sep31TransactionStore)
            .id(generateSepTransactionId())
            .status(SepTransactionStatus.PENDING_SENDER.toString())
            .statusEta(null)
            .amountFee(fee.getAmount())
            .amountFeeAsset(fee.getAsset())
            .startedAt(now)
            .updatedAt(now) // this will be overwritten by the sep31TransactionStore#save method.
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
            .amountExpected(request.getAmount())
            .amountIn(request.getAmount())
            .amountInAsset(assetInfo.getAssetName())
            .amountOut(null)
            .amountOutAsset(null)
            // updateDepositInfo will update these ⬇️
            .stellarAccountId(assetInfo.getDistributionAccount())
            .stellarMemo(null)
            .stellarMemoType(null)
            .build();

    Context.get().setTransaction(txn);
    updateAmounts();

    Context.get().setTransaction(sep31TransactionStore.save(txn));
    txn = Context.get().getTransaction();

    updateDepositInfo();

    StellarId senderStellarId = StellarId.builder().id(txn.getSenderId()).build();
    StellarId receiverStellarId = StellarId.builder().id(txn.getReceiverId()).build();
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
            .amountExpected(new Amount(txn.getAmountExpected(), txn.getAmountInAsset()))
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
            .sourceAccount(null)
            .destinationAccount(null)
            .customers(new Customers(senderStellarId, receiverStellarId))
            .creator(creatorStellarId)
            .build();
    eventService.publish(event);

    return Sep31PostTransactionResponse.builder()
        .id(txn.getId())
        .stellarAccountId(txn.getStellarAccountId())
        .stellarMemo(isEmpty(txn.getStellarMemo()) ? "" : txn.getStellarMemo())
        .stellarMemoType(
            isEmpty(txn.getStellarMemoType()) ? MEMO_NONE.name() : txn.getStellarMemoType())
        .build();
  }

  /**
   * Will update the amountIn, amountOut and amountFee, as well as the assets, taking into account
   * if quotes or if the {callbackApi}/fee endpoint was used.
   *
   * @throws AnchorException is something went wrong.
   */
  void updateAmounts() throws AnchorException {
    Sep31PostTransactionRequest request = Context.get().getRequest();
    if (request.getQuoteId() != null) {
      updateTxAmountsBasedOnQuote();
      return;
    }
    updateTxAmountsWhenNoQuoteWasUsed();
  }

  /**
   * updateTxAmountsBasedOnQuote will update the amountIn, amountOut and fee based on the quote.
   *
   * @throws ServerErrorException if the quote object is missing
   */
  void updateTxAmountsBasedOnQuote() throws ServerErrorException {
    Sep38Quote quote = Context.get().getQuote();
    if (quote == null) {
      infoF("Quote for transaction ({}) not found", Context.get().getTransaction().getId());
      throw new ServerErrorException("Quote not found.");
    }

    Sep31Transaction txn = Context.get().getTransaction();
    debugF("Updating transaction ({}) with quote ({})", txn.getId(), quote.getId());
    txn.setAmountInAsset(quote.getSellAsset());
    txn.setAmountIn(quote.getSellAmount());
    txn.setAmountExpected(quote.getSellAmount());
    txn.setAmountOutAsset(quote.getBuyAsset());
    txn.setAmountOut(quote.getBuyAmount());
    txn.setAmountFee(quote.getFee().getTotal());
    txn.setAmountFeeAsset(quote.getFee().getAsset());
  }

  /**
   * updateTxAmountsWhenNoQuoteWasUsed will update the transaction amountIn and amountOut based on
   * the request amount and the fee.
   */
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
    txn.setAmountExpected(formatAmount(amountIn, scale));
    txn.setAmountInAsset(reqAsset.getAssetName());
    txn.setAmountOut(formatAmount(amountOut, scale));
    txn.setAmountOutAsset(reqAsset.getAssetName());

    // Update fee
    String feeStr = formatAmount(fee, scale);
    txn.setAmountFee(feeStr);
    txn.setAmountFeeAsset(feeResponse.getAsset());
    Context.get().getFee().setAmount(feeStr);
  }

  /**
   * updateDepositInfo will populate the transaction's deposit information (stellar_account_id, memo
   * and memo_type), as provided by the sep31DepositInfoGenerator.
   */
  void updateDepositInfo() throws AnchorException {
    Sep31Transaction txn = Context.get().getTransaction();
    Sep31DepositInfo depositInfo = sep31DepositInfoGenerator.generate(txn);
    infoF("Updating transaction ({}) with depositInfo ({})", txn.getId(), depositInfo);
    txn.setStellarAccountId(depositInfo.getStellarAddress());
    txn.setStellarMemo(depositInfo.getMemo());
    txn.setStellarMemoType(isEmpty(depositInfo.getMemoType()) ? "none" : depositInfo.getMemoType());
  }

  public Sep31GetTransactionResponse getTransaction(String id) throws AnchorException {
    if (Objects.toString(id, "").isEmpty()) {
      info("Empty 'id'");
      throw new BadRequestException("'id' is empty");
    }

    Sep31Transaction txn = sep31TransactionStore.findByTransactionId(id);
    if (txn == null) {
      infoF("Transaction ({}) not found", id);
      throw new NotFoundException(String.format("transaction (id=%s) not found", id));
    }

    return txn.toSep31GetTransactionResponse();
  }

  @Transactional(rollbackOn = {AnchorException.class, RuntimeException.class})
  public Sep31GetTransactionResponse patchTransaction(Sep31PatchTransactionRequest request)
      throws AnchorException {
    if (request == null) {
      infoF("request cannot be null");
      throw new BadRequestException("request cannot be null");
    }

    if (Objects.toString(request.getId(), "").isEmpty()) {
      infoF("id cannot be null or empty");
      throw new BadRequestException("id cannot be null nor empty");
    }

    Context.reset();

    Sep31Transaction txn = sep31TransactionStore.findByTransactionId(request.getId());
    if (txn == null) {
      infoF("Transaction ({}) not found", request.getId());
      throw new NotFoundException(String.format("transaction (id=%s) not found", request.getId()));
    }
    Context.get().setTransaction(txn);

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
    return savedTxn.toSep31GetTransactionResponse();
  }

  /**
   * validatePatchTransactionFields will validate if the fields provided in the PATCH request are
   * expected by the transaction.
   *
   * @param txn is the Sep31Transaction already stored in the database.
   * @param request is the Sep31PatchTransactionRequest request
   * @throws BadRequestException if the stored request is not expecting any info update.
   * @throws BadRequestException if one of the provided fields is not being expected by the stored
   *     transaction.
   */
  void validatePatchTransactionFields(Sep31Transaction txn, Sep31PatchTransactionRequest request)
      throws BadRequestException {
    if (txn.getRequiredInfoUpdates() == null
        || txn.getRequiredInfoUpdates().getTransaction() == null) {
      infoF("Transaction ({}) is not expecting any updates", txn.getId());
      throw new BadRequestException(
          String.format("Transaction (%s) is not expecting any updates", txn.getId()));
    }

    Map<String, AssetInfo.Sep31TxnFieldSpec> expectedFields =
        txn.getRequiredInfoUpdates().getTransaction();
    Map<String, String> requestFields = request.getFields().getTransaction();

    // validate if any of the fields from the request is not expected in the transaction.
    for (String fieldName : requestFields.keySet()) {
      if (!expectedFields.containsKey(fieldName)) {
        infoF("{} is not a expected field", fieldName);
        throw new BadRequestException(String.format("[%s] is not a expected field", fieldName));
      }
    }
  }

  /**
   * preValidateQuote will validate if the requested asset supports/requires quotes.
   *
   * <p>If quotes are supported and a `quote_id` was provided, this method will: - fetch the quote
   * using the callbackAPI. - validate if the quote is valid. - validate if the transaction fields
   * are compliant with the quote fields. - update the Context with the quote data.
   *
   * @throws BadRequestException if quotes are required but none was used in the request.
   * @throws BadRequestException if a quote with the provided id could not be found.
   * @throws BadRequestException if the transaction `amount` is different from the quote
   *     `sell_amount`.
   * @throws BadRequestException if the transaction `asset` is different from the quote
   *     `sell_asset`.
   */
  void preValidateQuote() throws BadRequestException {
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
    String assetName = Context.get().getAsset().getAssetName();
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

  /**
   * updateFee will update the transaction fee. If a quote was used, it will get the quote info and
   * use the quote fees for it, otherwise it will call `GET {callbackAPI}/fee` to get the fee
   * information
   *
   * @throws SepValidationException if the quote is missing the `fee` field.
   * @throws AnchorException if something else goes wrong.
   */
  void updateFee() throws SepValidationException, AnchorException {
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
    String assetName = Context.get().getAsset().getAssetName();
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

  /**
   * validateSenderAndReceiver will validate if the SEP-31 sender and receiver exist and their
   * status is ACCEPTED.
   *
   * @throws BadRequestException if `sender_id` or `receiver_id` is empty.
   * @throws Sep31CustomerInfoNeededException if the SEP-12 customer does not exist or if its status
   *     is not ACCEPTED.
   * @throws AnchorException is something else went wrong.
   */
  private void validateSenderAndReceiver()
      throws AnchorException, BadRequestException, Sep31CustomerInfoNeededException {
    String receiverId = Context.get().getRequest().getReceiverId();
    if (receiverId == null) {
      infoF("'receiver_id' cannot be empty for request ({})", Context.get().getRequest());
      throw new BadRequestException("receiver_id cannot be empty.");
    }

    Sep12Operation sep12Operation = Context.get().getAsset().getSep31().getSep12();
    Sep12GetCustomerResponse receiver;

    // Fix for https://github.com/stellar/java-stellar-anchor-sdk/issues/717 (ANCHOR-149)
    // TODO: check if there is a better way to do it, i.e. request a type first for a customer OR
    // stop iteration after first hit.
    if (sep12Operation != null) {
      Set<String> types = sep12Operation.getReceiver().getTypes().keySet();
      List<Sep12GetCustomerRequest> requests =
          types.stream()
              .map(type -> Sep12GetCustomerRequest.builder().id(receiverId).type(type).build())
              .collect(Collectors.toList());
      List<Sep12GetCustomerResponse> responses = new ArrayList<>();

      for (Sep12GetCustomerRequest request : requests) {
        try {
          responses.add(this.customerIntegration.getCustomer(request));
        } catch (AnchorException e) {
          infoF("Failed to get customer (receiver) info with an error ({})", e);
        }
      }

      List<Sep12GetCustomerResponse> accepted =
          responses.stream()
              .filter(Objects::nonNull)
              .filter(x -> x.getStatus() == Sep12Status.ACCEPTED)
              .collect(Collectors.toList());

      if (accepted.isEmpty()) {
        infoF("Customer (receiver) info needed for request ({})", Context.get().getRequest());
        throw new Sep31CustomerInfoNeededException(types.toString());
      }
      if (accepted.size() != 1) {
        infoF(
            "Ambiguous customer info for request ({}) having multiple accepted responses ({})",
            Context.get().getRequest(),
            accepted);
        throw new Sep31AmbiguousCustomerInfoException(accepted.toString());
      }
    } else {
      Sep12GetCustomerRequest request = Sep12GetCustomerRequest.builder().id(receiverId).build();

      receiver = this.customerIntegration.getCustomer(request);
      if (receiver == null || receiver.getStatus() != Sep12Status.ACCEPTED) {
        infoF("Customer (receiver) info needed for request ({})", Context.get().getRequest());
        throw new Sep31CustomerInfoNeededException("sep31-receiver");
      }
    }

    String senderId = Context.get().getRequest().getSenderId();
    if (senderId == null) {
      infoF("'sender_id' cannot be empty for request ({})", Context.get().getRequest());
      throw new BadRequestException("sender_id cannot be empty.");
    }

    if (sep12Operation != null) {
      Set<String> types = sep12Operation.getSender().getTypes().keySet();
      List<Sep12GetCustomerRequest> requests =
          types.stream()
              .map(type -> Sep12GetCustomerRequest.builder().id(senderId).type(type).build())
              .collect(Collectors.toList());
      List<Sep12GetCustomerResponse> responses = new ArrayList<>();

      for (Sep12GetCustomerRequest request : requests) {
        try {
          responses.add(this.customerIntegration.getCustomer(request));
        } catch (AnchorException e) {
          infoF("Failed to get customer (sender) info with an error ({})", e);
        }
      }

      List<Sep12GetCustomerResponse> accepted =
          responses.stream()
              .filter(Objects::nonNull)
              .filter(x -> x.getStatus() == Sep12Status.ACCEPTED)
              .collect(Collectors.toList());

      if (accepted.isEmpty()) {
        infoF("Customer (sender) info needed for request ({})", Context.get().getRequest());
        throw new Sep31CustomerInfoNeededException(types.toString());
      }
      if (accepted.size() != 1) {
        infoF(
            "Ambiguous customer (sender) info for request ({}) having multiple accepted responses ({})",
            Context.get().getRequest(),
            accepted);
        throw new Sep31AmbiguousCustomerInfoException(accepted.toString());
      }
    } else {
      Sep12GetCustomerRequest request = Sep12GetCustomerRequest.builder().id(senderId).build();

      Sep12GetCustomerResponse sender = this.customerIntegration.getCustomer(request);
      if (sender == null || sender.getStatus() != Sep12Status.ACCEPTED) {
        infoF("Customer (sender) info needed for request ({})", Context.get().getRequest());
        throw new Sep31CustomerInfoNeededException("sep31-sender");
      }
    }
  }

  /**
   * validateRequiredFields will validate if the fields provided in the `POST /transactions` or
   * `PATCH /transactions/{id}` request body contains all the fields expected by the Anchor, and
   * pre-configured in the `app-config.app.assets`.
   *
   * @throws BadRequestException if the asset is invalid or id the fields are missing from the
   *     request
   * @throws Sep31MissingFieldException if not all fields were provided.
   */
  void validateRequiredFields() throws BadRequestException, Sep31MissingFieldException {
    AssetInfo assetInfo = Context.get().getAsset();
    if (assetInfo == null) {
      infoF("Missing asset information for request ({})", Context.get().getRequest());
      throw new BadRequestException("Missing asset information.");
    }

    AssetResponse fieldSpecs = this.infoResponse.getReceive().get(assetInfo.getCode());
    if (fieldSpecs == null) {
      infoF("Asset [{}] has no fields definition", Context.get().getRequest());
      throw new BadRequestException(
          String.format("Asset [%s] has no fields definition", assetInfo.getCode()));
    }

    Map<String, String> requestFields = Context.get().getTransactionFields();
    if (requestFields == null) {
      infoF(
          "'fields' field must have one 'transaction' field for request ({})",
          Context.get().getRequest());
      throw new BadRequestException("'fields' field must have one 'transaction' field");
    }

    Map<String, AssetInfo.Sep31TxnFieldSpec> missingFields =
        fieldSpecs.getFields().getTransaction().entrySet().stream()
            .filter(
                entry -> {
                  AssetInfo.Sep31TxnFieldSpec field = entry.getValue();
                  if (field.isOptional()) return false;
                  return requestFields.get(entry.getKey()) == null;
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

  @SneakyThrows
  private static Sep31InfoResponse sep31InfoResponseFromAssetInfoList(List<AssetInfo> assetInfos) {
    Sep31InfoResponse response = new Sep31InfoResponse();
    response.setReceive(new HashMap<>());
    for (AssetInfo assetInfo : assetInfos) {
      if (assetInfo.getSep31Enabled()) {
        boolean isQuotesSupported = assetInfo.getSep31().isQuotesSupported();
        boolean isQuotesRequired = assetInfo.getSep31().isQuotesRequired();
        if (isQuotesRequired && !isQuotesSupported) {
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
    private Sep31Transaction transaction;
    private Sep31PostTransactionRequest request;
    private Sep38Quote quote;
    private JwtToken jwtToken;
    private Amount fee;
    private AssetInfo asset;
    private Map<String, String> transactionFields;
    private static ThreadLocal<Context> context = new ThreadLocal<>();

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
}
