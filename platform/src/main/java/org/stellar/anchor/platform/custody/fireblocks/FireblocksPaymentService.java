package org.stellar.anchor.platform.custody.fireblocks;

import static org.stellar.anchor.util.MemoHelper.memoTypeAsString;

import com.google.gson.Gson;
import org.stellar.anchor.api.custody.GenerateDepositAddressResponse;
import org.stellar.anchor.api.exception.FireblocksException;
import org.stellar.anchor.platform.config.FireblocksConfig;
import org.stellar.anchor.platform.custody.PaymentService;
import org.stellar.anchor.platform.dto.fireblocks.CreateNewDepositAddressRequestDto;
import org.stellar.anchor.platform.dto.fireblocks.CreateNewDepositAddressResponseDto;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.sdk.xdr.MemoType;

public class FireblocksPaymentService implements PaymentService {

  private static final Gson gson = GsonUtils.getInstance();

  private static final String CREATE_NEW_DEPOSIT_ADDRESS_URL_FORMAT =
      "/vault/accounts/%s/%s/addresses";

  private final FireblocksClient fireblocksClient;
  private final FireblocksConfig fireblocksConfig;

  public FireblocksPaymentService(
      FireblocksClient fireblocksClient, FireblocksConfig fireblocksConfig) {
    this.fireblocksClient = fireblocksClient;
    this.fireblocksConfig = fireblocksConfig;
  }

  @Override
  public GenerateDepositAddressResponse generateDepositAddress(String assetId)
      throws FireblocksException {
    CreateNewDepositAddressRequestDto request = CreateNewDepositAddressRequestDto.builder().build();
    CreateNewDepositAddressResponseDto depositAddress =
        gson.fromJson(
            fireblocksClient.post(
                String.format(
                    CREATE_NEW_DEPOSIT_ADDRESS_URL_FORMAT,
                    fireblocksConfig.getVaultAccountId(),
                    assetId),
                gson.toJson(request)),
            CreateNewDepositAddressResponseDto.class);
    return GenerateDepositAddressResponse.builder()
        .address(depositAddress.getAddress())
        .memo(depositAddress.getTag())
        .memoType(memoTypeAsString(MemoType.MEMO_ID))
        .build();
  }
}
