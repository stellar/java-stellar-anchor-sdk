package org.stellar.anchor.platform.service;

import org.stellar.anchor.api.sep.sep31.Sep31DepositInfo;
import org.stellar.anchor.platform.payment.observer.circle.CirclePaymentService;
import org.stellar.anchor.platform.payment.observer.circle.model.CircleBlockchainAddress;
import org.stellar.anchor.sep31.Sep31DepositInfoGenerator;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.xdr.MemoType;

public class Sep31DepositInfoGeneratorCircle implements Sep31DepositInfoGenerator {

  private final CirclePaymentService circlePaymentService;

  public Sep31DepositInfoGeneratorCircle(CirclePaymentService circlePaymentService) {
    this.circlePaymentService = circlePaymentService;
  }

  @Override
  public Sep31DepositInfo generate(Sep31Transaction txn) {
    return circlePaymentService
        .getDistributionAccountAddress()
        .flatMap(circlePaymentService::createNewStellarAddress)
        .map(
            circleBlockchainAddressCircleDetailResponse -> {
              CircleBlockchainAddress blockchainAddress =
                  circleBlockchainAddressCircleDetailResponse.getData();
              return new Sep31DepositInfo(
                  blockchainAddress.getAddress(),
                  blockchainAddress.getAddressTag(),
                  MemoHelper.memoTypeAsString(MemoType.MEMO_TEXT));
            })
        .block();
  }
}
