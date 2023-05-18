package org.stellar.anchor.platform.custody.fireblocks;

import static org.stellar.anchor.api.custody.fireblocks.CreateTransactionRequest.DestinationTransferPeerPathType.ONE_TIME_ADDRESS;
import static org.stellar.anchor.api.custody.fireblocks.CreateTransactionRequest.TransferPeerPathType.VAULT_ACCOUNT;
import static org.stellar.anchor.util.MemoHelper.memoTypeAsString;

import com.google.gson.Gson;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.stellar.anchor.api.custody.CreateTransactionPaymentResponse;
import org.stellar.anchor.api.custody.GenerateDepositAddressResponse;
import org.stellar.anchor.api.custody.fireblocks.CreateAddressRequest;
import org.stellar.anchor.api.custody.fireblocks.CreateAddressResponse;
import org.stellar.anchor.api.custody.fireblocks.CreateTransactionRequest;
import org.stellar.anchor.api.custody.fireblocks.CreateTransactionResponse;
import org.stellar.anchor.api.custody.fireblocks.TransactionDetails;
import org.stellar.anchor.api.exception.FireblocksException;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.platform.config.FireblocksConfig;
import org.stellar.anchor.platform.custody.CustodyPaymentService;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.sdk.xdr.MemoType;

public class FireblocksPaymentService implements CustodyPaymentService {

  private static final Gson gson = GsonUtils.getInstance();

  private static final String CREATE_NEW_DEPOSIT_ADDRESS_URL_FORMAT =
      "/v1/vault/accounts/%s/%s/addresses";
  private static final String CREATE_NEW_TRANSACTION_PAYMENT_URL = "/v1/transactions";
  private static final String GET_TRANSACTION_BY_ID_URL_FORMAT = "/v1/transactions/%s";

  private final FireblocksApiClient fireblocksApiClient;
  private final FireblocksConfig fireblocksConfig;

  public FireblocksPaymentService(
      FireblocksApiClient fireblocksApiClient, FireblocksConfig fireblocksConfig) {
    this.fireblocksApiClient = fireblocksApiClient;
    this.fireblocksConfig = fireblocksConfig;
  }

  @Override
  public GenerateDepositAddressResponse generateDepositAddress(String assetId)
      throws FireblocksException, InvalidConfigException {
    CreateAddressRequest request = new CreateAddressRequest();
    CreateAddressResponse depositAddress =
        gson.fromJson(
            fireblocksApiClient.post(
                String.format(
                    CREATE_NEW_DEPOSIT_ADDRESS_URL_FORMAT,
                    fireblocksConfig.getVaultAccountId(),
                    fireblocksConfig.getFireblocksAssetCode(assetId)),
                gson.toJson(request)),
            CreateAddressResponse.class);
    return new GenerateDepositAddressResponse(
        depositAddress.getAddress(), depositAddress.getTag(), memoTypeAsString(MemoType.MEMO_ID));
  }

  @Retryable(
      value = FireblocksException.class,
      maxAttemptsExpression = "${custody.fireblocks.retry_config.max_attempts}",
      backoff = @Backoff(delayExpression = "${custody.fireblocks.retry_config.delay}"),
      exceptionExpression =
          "#root instanceof T(org.stellar.anchor.api.exception.FireblocksException) AND"
              + "(#root.statusCode == 429 OR #root.statusCode == 503)")
  public CreateTransactionPaymentResponse createTransactionPayment(
      JdbcCustodyTransaction txn, String requestBody)
      throws FireblocksException, InvalidConfigException {
    CreateTransactionRequest request = getCreateTransactionRequest(txn);

    CreateTransactionResponse response =
        gson.fromJson(
            fireblocksApiClient.post(CREATE_NEW_TRANSACTION_PAYMENT_URL, gson.toJson(request)),
            CreateTransactionResponse.class);

    return new CreateTransactionPaymentResponse(response.getId());
  }

  public CreateTransactionRequest getCreateTransactionRequest(JdbcCustodyTransaction txn)
      throws InvalidConfigException {
    return CreateTransactionRequest.builder()
        .assetId(fireblocksConfig.getFireblocksAssetCode(txn.getAmountOutAsset()))
        .amount(txn.getAmountOut())
        .source(
            new CreateTransactionRequest.TransferPeerPath(
                VAULT_ACCOUNT, fireblocksConfig.getVaultAccountId()))
        .destination(
            new CreateTransactionRequest.DestinationTransferPeerPath(
                ONE_TIME_ADDRESS, new CreateTransactionRequest.OneTimeAddress(txn.getToAccount())))
        .build();
  }

  @Override
  public TransactionDetails getTransactionById(String txnId) throws FireblocksException {
    return gson.fromJson(
        fireblocksApiClient.get(String.format(GET_TRANSACTION_BY_ID_URL_FORMAT, txnId)),
        TransactionDetails.class);
  }
}
