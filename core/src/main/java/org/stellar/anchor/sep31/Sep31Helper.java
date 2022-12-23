package org.stellar.anchor.sep31;

import static org.stellar.anchor.util.SepHelper.validateTransactionStatus;

import java.util.UUID;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.api.shared.StellarId;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.event.models.TransactionEvent;

public class Sep31Helper {
  public static boolean allAmountAvailable(Sep31Transaction txn) {
    return txn.getAmountIn() != null
        && txn.getAmountInAsset() != null
        && txn.getAmountFee() != null
        && txn.getAmountFeeAsset() != null
        && txn.getAmountOut() != null
        && txn.getAmountOutAsset() != null;
  }

  public static void validateStatus(Sep31Transaction txn) throws BadRequestException {
    if (!validateTransactionStatus(txn.getStatus(), 31)) {
      throw new BadRequestException(
          String.format("'%s' is not a valid status of SEP31.", txn.getStatus()));
    }
  }

  public static void publishEvent(
      EventService eventService, Sep31Transaction txn, TransactionEvent.Type eventType)
      throws EventPublishException {
    StellarId senderStellarId = StellarId.builder().id(txn.getSenderId()).build();
    StellarId receiverStellarId = StellarId.builder().id(txn.getReceiverId()).build();
    TransactionEvent event =
        TransactionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .type(eventType)
            .id(txn.getId())
            .sep(TransactionEvent.Sep.SEP_31)
            .kind(TransactionEvent.Kind.RECEIVE)
            .status(TransactionEvent.Status.from(txn.getStatus()))
            .build();
    eventService.publish(event);
  }
}
