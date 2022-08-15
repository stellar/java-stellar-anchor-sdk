package org.stellar.anchor.platform.payment.observer.circle.model;

import lombok.Data;
import org.stellar.anchor.platform.payment.common.DepositInstructions;
import org.stellar.anchor.platform.payment.common.PaymentNetwork;
import org.stellar.anchor.platform.payment.observer.circle.util.CircleAsset;
import org.stellar.sdk.Network;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

@Data
public class CircleBlockchainAddress {
  @NonNull String address;
  @Nullable String addressTag;
  @NonNull String currency;
  @NonNull String chain;

  public CircleBlockchainAddress(
      @NonNull String address,
      @Nullable String addressTag,
      @NonNull String currency,
      @NonNull String chain) {
    this.address = address;
    this.addressTag = addressTag;
    this.currency = currency;
    this.chain = chain;
  }

  public DepositInstructions toDepositInstructions(
      String beneficiaryAccountId, Network stellarNetwork) {
    return new DepositInstructions(
        beneficiaryAccountId,
        null,
        PaymentNetwork.CIRCLE,
        address,
        addressTag,
        PaymentNetwork.STELLAR,
        CircleAsset.stellarUSDC(stellarNetwork),
        null);
  }
}
