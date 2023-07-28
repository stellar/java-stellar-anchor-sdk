package org.stellar.anchor.util;

public enum MetricNames {
  // SEP-1 metrics
  SEP1_TOML_ACCESSED("sep1.toml_accessed"),

  // SEP-6 metrics
  SEP6_TRANSACTION("sep6.transaction"),
  TV_SEP6_DEPOSIT("deposit"),
  TV_SEP6_WITHDRAWAL("withdrawal"),

  // SEP-10 metrics
  SEP10_CHALLENGE_CREATED("sep10.transaction"),
  SEP10_CHALLENGE_VALIDATED("sep10.challenge.signed"),

  // SEP-12 metrics
  SEP12_CUSTOMER("sep12.customer"),
  TV_SEP12_GET_CUSTOMER("get"),
  TV_SEP12_PUT_CUSTOMER("put"),
  TV_SEP12_DELETE_CUSTOMER("delete"),

  // SEP-24 metrics
  SEP24_TRANSACTION_REQUESTED("sep24.transaction.requested"),
  SEP24_TRANSACTION_CREATED("sep24.transaction.created"),
  SEP24_TRANSACTION_QUERIED("sep24.transaction.queried"),
  TV_SEP24_WITHDRAWAL("withdrawal"),
  TV_SEP24_DEPOSIT("deposit"),

  // SEP-31 metrics
  SEP31_TRANSACTION_REQUESTED("sep31.transaction.requested"),
  SEP31_TRANSACTION_CREATED("sep31.transaction.requested"),
  SEP31_TRANSACTION_PATCHED("sep31.transaction.patched"),

  // SEP-38 metrics
  SEP38_PRICE_QUERIED("sep38.price.queried"),
  SEP38_QUOTE_CREATED("sep38.quote.created"),

  // payment observer metrics
  PAYMENT_OBSERVER("payment_observer"),
  TV_PAYMENT_RECEIVED("payment_received"),

  // Logger metrics
  LOGGER("logger"),

  TYPE("type"),
  STATUS("status");

  private final String name;

  MetricNames(String name) {
    this.name = name;
  }
}
