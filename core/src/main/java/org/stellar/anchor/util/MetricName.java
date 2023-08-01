package org.stellar.anchor.util;

public enum MetricName {
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
  PAYMENT_OBSERVER_LATEST_BLOCK_READ("payment_observer.latest_block_read"),
  PAYMENT_OBSERVER_LATEST_BLOCK_PROCESSED("payment_observer.latest_block_processed"),

  // event processor metrics
  EVENT_RECEIVED("event_processor.event_received"),
  EVENT_PROCESSED("event_processor.event_processed"),
  TV_BUSINESS_SERVER_CALLBACK("business_server_callback_api"),
  TV_STATUS_CALLBACK("status_callback"),
  TV_KNOWN("known"),

  // platform server metrics
  PLATFORM_PATCH_TRANSACTION("platform_server.patch_transaction"),
  PLATFORM_FIND_TRANSACTION("platform_server.get_transaction"),
  PLATFORM_FIND_TRANSACTIONS("platform_server.get_transactions"),
  TV_SEP6("sep6"),
  TV_SEP24("sep24"),
  TV_SEP31("sep31"),

  // Logger metrics
  LOGGER("logger"),

  // Tag names
  SEP("SEP"),
  QUEUE("queue"),
  STATUS("status"),
  TYPE("type");

  private final String name;

  MetricName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
