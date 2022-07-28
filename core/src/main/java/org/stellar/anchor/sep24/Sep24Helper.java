package org.stellar.anchor.sep24;

import static javax.print.attribute.standard.JobState.COMPLETED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.*;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.MathHelper.decimal;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.BeanUtils;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep24.*;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.util.DateUtil;

public class Sep24Helper {
  private static final List<String> needsMoreInfoUrlDeposit =
      Arrays.asList(
          PENDING_USR_TRANSFER_START.toString(),
          PENDING_USR_TRANSFER_COMPLETE.toString(),
          COMPLETED.toString(),
          REFUNDED.toString(),
          PENDING_EXTERNAL.toString(),
          PENDING_ANCHOR.toString(),
          PENDING_USER.toString());
  private static final List<String> needsMoreInfoUrlWithdraw =
      Arrays.asList(
          PENDING_USR_TRANSFER_START.toString(),
          PENDING_USR_TRANSFER_COMPLETE.toString(),
          COMPLETED.toString(),
          REFUNDED.toString(),
          PENDING_EXTERNAL.toString(),
          PENDING_ANCHOR.toString(),
          PENDING_USER.toString());

  public static String constructMoreInfoUrl(
      JwtService jwtService, Sep24Config sep24Config, Sep24Transaction txn, String lang)
      throws URISyntaxException, MalformedURLException {

    JwtToken token =
        JwtToken.of(
            "moreInfoUrl",
            (txn.getSep10AccountMemo() == null || txn.getSep10AccountMemo().length() == 0)
                ? txn.getSep10Account()
                : txn.getSep10Account() + ":" + txn.getSep10AccountMemo(),
            Instant.now().getEpochSecond(),
            Instant.now().getEpochSecond() + sep24Config.getInteractiveJwtExpiration(),
            txn.getTransactionId(),
            txn.getClientDomain());

    URI uri = new URI(sep24Config.getInteractiveUrl());

    URIBuilder builder =
        new URIBuilder()
            .setScheme(uri.getScheme())
            .setHost(uri.getHost())
            .setPort(uri.getPort())
            .setPath("transaction-status")
            .addParameter("transaction_id", txn.getTransactionId())
            .addParameter("token", jwtService.encode(token));

    if (lang != null) {
      builder.addParameter("lang", lang);
    }

    return builder.build().toURL().toString();
  }

  public static TransactionResponse fromDepositTxn(
      JwtService jwtService,
      Sep24Config sep24Config,
      Sep24Transaction txn,
      String lang,
      boolean allowMoreInfoUrl)
      throws MalformedURLException, URISyntaxException {

    DepositTransactionResponse txnR = new DepositTransactionResponse();
    BeanUtils.copyProperties(txn, txnR);

    txnR.setId(txn.getTransactionId());
    txnR.setDepositMemo(txn.getMemo());
    txnR.setDepositMemoType(txn.getMemoType());
    txnR.setFrom(txn.getFromAccount());
    txnR.setTo(txn.getToAccount());
    txnR.setDepositMemo(txn.getMemo());
    txnR.setDepositMemoType(txn.getMemoType());

    txnR.setStartedAt(
        (txn.getStartedAt() == null) ? null : DateUtil.toISO8601UTC(txn.getStartedAt()));
    txnR.setCompletedAt(
        (txn.getCompletedAt() == null) ? null : DateUtil.toISO8601UTC(txn.getCompletedAt()));

    if (allowMoreInfoUrl && needsMoreInfoUrlDeposit.contains(txn.getStatus())) {
      txnR.setMoreInfoUrl(constructMoreInfoUrl(jwtService, sep24Config, txn, lang));
    } else {
      txnR.setMoreInfoUrl(null);
    }

    return txnR;
  }

  public static WithdrawTransactionResponse fromWithdrawTxn(
      JwtService jwtService,
      Sep24Config sep24Config,
      Sep24Transaction txn,
      String lang,
      boolean allowMoreInfoUrl)
      throws MalformedURLException, URISyntaxException {

    WithdrawTransactionResponse txnR = new WithdrawTransactionResponse();
    BeanUtils.copyProperties(txn, txnR);

    txnR.setStartedAt(
        (txn.getStartedAt() == null) ? null : DateUtil.toISO8601UTC(txn.getStartedAt()));
    txnR.setCompletedAt(
        (txn.getCompletedAt() == null) ? null : DateUtil.toISO8601UTC(txn.getCompletedAt()));
    txnR.setId(txn.getTransactionId());
    txnR.setFrom(txn.getFromAccount());
    txnR.setTo(txn.getToAccount());

    txnR.setWithdrawMemo(txn.getMemo());
    txnR.setWithdrawMemoType(txn.getMemoType());
    txnR.setWithdrawAnchorAccount(txn.getWithdrawAnchorAccount());

    if (allowMoreInfoUrl && needsMoreInfoUrlWithdraw.contains(txn.getStatus())) {
      txnR.setMoreInfoUrl(constructMoreInfoUrl(jwtService, sep24Config, txn, lang));
    } else {
      txnR.setMoreInfoUrl(null);
    }

    return txnR;
  }

  public static TransactionResponse updateRefundInfo(
      TransactionResponse response, Sep24Transaction txn, AssetInfo assetInfo) {
    debugF("Calculating refund information");

    List<? extends Sep24RefundPayment> refundPayments = txn.getRefundPayments();
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
}
