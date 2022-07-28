package org.stellar.anchor.platform.service;

import java.util.Objects;
import org.stellar.anchor.api.callback.GetUniqueAddressResponse;
import org.stellar.anchor.api.callback.UniqueAddressIntegration;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.sep.sep31.Sep31DepositInfo;
import org.stellar.anchor.sep31.Sep31DepositInfoGenerator;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.util.MemoHelper;

public class Sep31DepositInfoGeneratorApi implements Sep31DepositInfoGenerator {
  private UniqueAddressIntegration uniqueAddressIntegration;

  public Sep31DepositInfoGeneratorApi(UniqueAddressIntegration uniqueAddressIntegration) {
    this.uniqueAddressIntegration = uniqueAddressIntegration;
  }

  @Override
  public Sep31DepositInfo generate(Sep31Transaction txn) throws AnchorException {
    GetUniqueAddressResponse response = uniqueAddressIntegration.getUniqueAddress(txn.getId());
    GetUniqueAddressResponse.UniqueAddress uniqueAddress = response.getUniqueAddress();

    if (!Objects.toString(uniqueAddress.getMemo(), "").isEmpty()
        && !Objects.toString(uniqueAddress.getMemoType(), "").isEmpty()) {
      // Check the validity of the returned memo and memo type
      MemoHelper.makeMemo(uniqueAddress.getMemo(), uniqueAddress.getMemoType());
    }

    return new Sep31DepositInfo(
        uniqueAddress.getStellarAddress(), uniqueAddress.getMemo(), uniqueAddress.getMemoType());
  }
}
