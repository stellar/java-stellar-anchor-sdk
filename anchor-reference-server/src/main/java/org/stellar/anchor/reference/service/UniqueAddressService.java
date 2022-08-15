package org.stellar.anchor.reference.service;

import static org.stellar.anchor.util.MemoHelper.memoTypeAsString;
import static org.stellar.anchor.util.StringHelper.isEmpty;
import static org.stellar.sdk.xdr.MemoType.MEMO_HASH;

import java.util.Base64;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.callback.GetUniqueAddressResponse;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.util.ConditionalOnPropertyNotEmpty;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.KeyPair;

@Service
@ConditionalOnPropertyNotEmpty("anchor.settings.distributionWallet")
public class UniqueAddressService {
  AppSettings appSettings;

  UniqueAddressService(AppSettings appSettings) throws SepException {
    this.appSettings = appSettings;

    if (Objects.toString(appSettings.getDistributionWallet(), "").isEmpty()) {
      throw new SepException("distributionWallet is empty");
    }

    try {
      KeyPair.fromAccountId(appSettings.getDistributionWallet());
    } catch (Exception ex) {
      throw new SepException(
          String.format("Invalid distributionWallet: [%s]", appSettings.getDistributionWallet()));
    }

    if (!isEmpty(appSettings.getDistributionWalletMemo())
        && !isEmpty(appSettings.getDistributionWalletMemoType())) {
      // check if memo and memoType are valid
      MemoHelper.makeMemo(
          appSettings.getDistributionWalletMemo(), appSettings.getDistributionWalletMemoType());
    }
  }

  public GetUniqueAddressResponse getUniqueAddress(String transactionId) {
    // transactionId may be used to query the transaction information if the anchor would like to
    // return a transaction-dependent unique address.

    Log.debugF("Getting a unique address for transaction[id={}]", transactionId);
    GetUniqueAddressResponse.UniqueAddress.UniqueAddressBuilder uniqueAddressBuilder =
        GetUniqueAddressResponse.UniqueAddress.builder()
            .stellarAddress(appSettings.getDistributionWallet());

    if (isEmpty(appSettings.getDistributionWalletMemo())
        || isEmpty(appSettings.getDistributionWalletMemoType())) {
      String memo = StringUtils.truncate(transactionId, 32);
      memo = StringUtils.leftPad(memo, 32, '0');
      memo = new String(Base64.getEncoder().encode(memo.getBytes()));
      uniqueAddressBuilder.memo(memo).memoType(memoTypeAsString(MEMO_HASH));
    } else {
      uniqueAddressBuilder
          .memo(appSettings.getDistributionWalletMemo())
          .memoType(appSettings.getDistributionWalletMemoType());
    }

    GetUniqueAddressResponse resp =
        GetUniqueAddressResponse.builder().uniqueAddress(uniqueAddressBuilder.build()).build();
    Log.infoF(
        "Got the unique address for transaction[id={}]. memo={}, memoType={}",
        transactionId,
        resp.getUniqueAddress().getMemo(),
        resp.getUniqueAddress().getMemoType());

    return resp;
  }
}
