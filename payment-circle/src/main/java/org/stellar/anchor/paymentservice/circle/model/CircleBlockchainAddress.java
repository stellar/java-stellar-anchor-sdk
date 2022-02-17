package org.stellar.anchor.paymentservice.circle.model;

import lombok.Data;
import org.stellar.anchor.paymentservice.DepositInstructions;
import org.stellar.anchor.paymentservice.PaymentNetwork;
import org.stellar.anchor.paymentservice.circle.util.CircleAsset;
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
    String intermediaryCurrencyName =
        PaymentNetwork.STELLAR.getCurrencyPrefix() + ":" + CircleAsset.stellarUSDC(stellarNetwork);
    return new DepositInstructions(
        beneficiaryAccountId,
        null,
        PaymentNetwork.CIRCLE,
        address,
        addressTag,
        PaymentNetwork.STELLAR,
        intermediaryCurrencyName,
        null);
  }
}
