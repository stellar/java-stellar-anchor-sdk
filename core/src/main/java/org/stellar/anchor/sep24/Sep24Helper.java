package org.stellar.anchor.sep24;

import static org.stellar.anchor.api.sep.SepTransactionStatus.*;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.MathHelper.decimal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.BeanUtils;
import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep24.RefundPayment;
import org.stellar.anchor.api.sep.sep24.Refunds;
import org.stellar.anchor.api.sep.sep24.TransactionResponse;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.event.models.TransactionEvent;

public class Sep24Helper {
  static final List<String> needsMoreInfoUrlDeposit =
      Arrays.asList(
          PENDING_USR_TRANSFER_START.toString(),
          PENDING_USR_TRANSFER_COMPLETE.toString(),
          COMPLETED.toString(),
          REFUNDED.toString(),
          PENDING_EXTERNAL.toString(),
          PENDING_ANCHOR.toString(),
          PENDING_USER.toString());
  static final List<String> needsMoreInfoUrlWithdraw =
      Arrays.asList(
          PENDING_USR_TRANSFER_START.toString(),
          PENDING_USR_TRANSFER_COMPLETE.toString(),
          COMPLETED.toString(),
          REFUNDED.toString(),
          PENDING_EXTERNAL.toString(),
          PENDING_ANCHOR.toString(),
          PENDING_USER.toString());

  static void setSharedTransactionResponseFields(TransactionResponse txnR, Sep24Transaction txn) {
    txnR.setId(txn.getTransactionId());
    if (txn.getFromAccount() != null) txnR.setFrom(txn.getFromAccount());
    if (txn.getToAccount() != null) txnR.setTo(txn.getToAccount());
    if (txn.getStartedAt() != null) txnR.setStartedAt(txn.getStartedAt());
    if (txn.getCompletedAt() != null) txnR.setCompletedAt(txn.getCompletedAt());
  }

  static TransactionResponse updateRefundInfo(
      TransactionResponse response, Sep24Transaction txn, AssetInfo assetInfo) {
    debugF("Calculating refund information");

    if (txn.getRefunds() == null) return response;

    List<Sep24RefundPayment> refundPayments = txn.getRefunds().getRefundPayments();
    response.setRefunded(false);
    BigDecimal totalAmount = BigDecimal.ZERO;
    BigDecimal totalFee = BigDecimal.ZERO;

    if (refundPayments != null && refundPayments.size() > 0) {
      debugF("{} refund payments found", refundPayments.size());
      List<RefundPayment> rps = new ArrayList<>(refundPayments.size());
      for (Sep24RefundPayment refundPayment : refundPayments) {
        if (refundPayment.getAmount() != null)
          totalAmount = totalAmount.add(decimal(refundPayment.getAmount(), assetInfo));
        if (refundPayment.getFee() != null)
          totalFee = totalFee.add(decimal(refundPayment.getFee(), assetInfo));
        RefundPayment rp = new RefundPayment();
        BeanUtils.copyProperties(refundPayment, rp);
        rps.add(rp);
      }
      Refunds refunds =
          Refunds.builder()
              .amountRefunded(totalAmount.toString())
              .amountFee(totalFee.toString())
              .payments(rps)
              .build();
      response.setRefunds(refunds);

      if (totalAmount.equals(decimal(response.getAmountIn(), assetInfo))) {
        response.setRefunded(true);
      }
    }

    return response;
  }

  public static void publishEvent(
      EventService eventService, Sep24Transaction txn, TransactionEvent.Type eventType)
      throws EventPublishException {
    TransactionEvent event =
        TransactionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .type(eventType)
            .id(txn.getId())
            .sep(TransactionEvent.Sep.SEP_24)
            .kind(TransactionEvent.Kind.from(txn.getKind()))
            .status(TransactionEvent.Status.from(txn.getStatus()))
            .build();
    eventService.publish(event);
  }

  public static JwtToken buildRedirectJwtToken(
      Sep24Config sep24Config, String fullRequestUrl, JwtToken token, Sep24Transaction txn) {
    return JwtToken.of(
        fullRequestUrl,
        token.getSub(),
        Instant.now().getEpochSecond(),
        Instant.now().getEpochSecond() + sep24Config.getInteractiveJwtExpiration(),
        txn.getTransactionId(),
        token.getClientDomain());
  }
}
