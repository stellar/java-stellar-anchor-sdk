package org.stellar.anchor.event.models;

public interface AnchorEvent {
  String TYPE_TRANSACTION = "TransactionEvent";
  String TYPE_QUOTE = "QuoteEvent";

  String getType();

  String getEventId();
}
