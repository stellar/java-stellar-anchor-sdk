package org.stellar.anchor.platform.service;

import static org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountsManager.AccountType.TRANSIENT;

import java.util.Objects;
import org.stellar.anchor.api.callback.GetUniqueAddressResponse;
import org.stellar.anchor.api.callback.UniqueAddressIntegration;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.InternalServerErrorException;
import org.stellar.anchor.api.shared.SepDepositInfo;
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountsManager;
import org.stellar.anchor.sep31.Sep31DepositInfoGenerator;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.util.MemoHelper;

public class Sep31DepositInfoApiGenerator implements Sep31DepositInfoGenerator {

  private final UniqueAddressIntegration uniqueAddressIntegration;
  private final PaymentObservingAccountsManager paymentObservingAccountsManager;

  public Sep31DepositInfoApiGenerator(
      UniqueAddressIntegration uniqueAddressIntegration,
      PaymentObservingAccountsManager paymentObservingAccountsManager) {
    this.uniqueAddressIntegration = uniqueAddressIntegration;
    this.paymentObservingAccountsManager = paymentObservingAccountsManager;
  }

  @Override
  public SepDepositInfo generate(Sep31Transaction txn) throws AnchorException {
    GetUniqueAddressResponse response = uniqueAddressIntegration.getUniqueAddress(txn.getId());
    GetUniqueAddressResponse.UniqueAddress uniqueAddress = response.getUniqueAddress();

    if (uniqueAddress == null) {
      throw new InternalServerErrorException(
          "The server does not respond with a unique address for SEP31 deposit");
    }

    if (!Objects.toString(uniqueAddress.getMemo(), "").isEmpty()
        && !Objects.toString(uniqueAddress.getMemoType(), "").isEmpty()) {
      // Check the validity of the returned memo and memo type
      MemoHelper.makeMemo(uniqueAddress.getMemo(), uniqueAddress.getMemoType());
    }

    // Add to payment observer manager so that we get events of the stellar address.
    paymentObservingAccountsManager.upsert(uniqueAddress.getStellarAddress(), TRANSIENT);

    return new SepDepositInfo(
        uniqueAddress.getStellarAddress(), uniqueAddress.getMemo(), uniqueAddress.getMemoType());
  }
}
