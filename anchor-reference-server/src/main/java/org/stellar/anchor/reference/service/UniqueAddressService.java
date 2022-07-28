package org.stellar.anchor.reference.service;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.callback.GetUniqueAddressResponse;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.KeyPair;

@Service
public class UniqueAddressService {
  String distributionWallet;
  String distributionWalletMemo;
  String distributionWalletMemoType;

  UniqueAddressService(AppSettings appSettings) throws SepException {
    this.distributionWallet = appSettings.getDistributionWallet();
    if (Objects.toString(appSettings.getDistributionWallet(), "").isEmpty()) {
      throw new SepException("distributionWallet is empty");
    }

    try {
      KeyPair.fromAccountId(this.distributionWallet);
    } catch (Exception ex) {
      throw new SepException(
          String.format("Invalid distributionWallet: [%s]", this.distributionWallet));
    }

    if (!Objects.toString(appSettings.getDistributionWalletMemo(), "").isEmpty()
        && !Objects.toString(appSettings.getDistributionWalletMemoType(), "").isEmpty()) {
      // check if memo and memoType are valid
      MemoHelper.makeMemo(
          appSettings.getDistributionWalletMemo(), appSettings.getDistributionWalletMemoType());
    }
    this.distributionWalletMemo = appSettings.getDistributionWalletMemo();
    this.distributionWalletMemoType = appSettings.getDistributionWalletMemoType();
  }

  public GetUniqueAddressResponse getUniqueAddress(String transactionId) {
    // transactionId may be used to query the transaction information if the anchor would like to
    // return a transaction-dependent unique address.
    return GetUniqueAddressResponse.builder()
        .uniqueAddress(
            GetUniqueAddressResponse.UniqueAddress.builder()
                .stellarAddress(distributionWallet)
                .memo(distributionWalletMemo)
                .memoType(distributionWalletMemoType)
                .build())
        .build();
  }
}
