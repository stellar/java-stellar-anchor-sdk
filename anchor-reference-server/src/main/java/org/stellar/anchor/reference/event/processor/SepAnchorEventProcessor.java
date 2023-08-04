package org.stellar.anchor.reference.event.processor;

import org.stellar.anchor.api.event.AnchorEvent;

/** Processes SEP-specific {@link AnchorEvent}. */
public interface SepAnchorEventProcessor {

  /**
   * Processes a {@link AnchorEvent} of type {@link AnchorEvent.Type#QUOTE_CREATED}.
   *
   * @param event the event to process
   */
  void onQuoteCreatedEvent(AnchorEvent event);

  /**
   * Processes a {@link AnchorEvent} of type {@link AnchorEvent.Type#TRANSACTION_CREATED}.
   *
   * @param event the event to process
   */
  void onTransactionCreated(AnchorEvent event);

  /**
   * Processes a {@link AnchorEvent} of type {@link AnchorEvent.Type#TRANSACTION_ERROR}.
   *
   * @param event the event to process
   */
  void onTransactionError(AnchorEvent event);

  /**
   * Processes a {@link AnchorEvent} of type {@link AnchorEvent.Type#TRANSACTION_STATUS_CHANGED}.
   *
   * @param event the event to process
   */
  void onTransactionStatusChanged(AnchorEvent event);

  /**
   * Processes a {@link AnchorEvent} of type {@link AnchorEvent.Type#CUSTOMER_UPDATED}.
   *
   * <p>Note: This is only used for SEP-6 currently.
   *
   * @param event the event to process
   */
  void onCustomerUpdated(AnchorEvent event);
}
