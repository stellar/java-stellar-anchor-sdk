package org.stellar.anchor.paymentservice.circle.model;

import lombok.Data;
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
}
