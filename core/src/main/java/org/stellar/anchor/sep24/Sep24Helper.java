package org.stellar.anchor.sep24;

import static javax.print.attribute.standard.JobState.COMPLETED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.*;

import com.google.gson.Gson;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.sep.sep24.DepositTransactionResponse;
import org.stellar.anchor.api.sep.sep24.TransactionResponse;
import org.stellar.anchor.api.sep.sep24.WithdrawTransactionResponse;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.util.DateUtil;

public class Sep24Helper {
  private static List<String> needsMoreInfoUrlDeposit =
      Arrays.asList(
          PENDING_USR_TRANSFER_START.toString(),
          PENDING_USR_TRANSFER_COMPLETE.toString(),
          COMPLETED.toString(),
          PENDING_EXTERNAL.toString(),
          PENDING_ANCHOR.toString(),
          PENDING_USER.toString());
  private static List<String> needsMoreInfoUrlWithdraw =
      Arrays.asList(
          PENDING_USR_TRANSFER_START.toString(),
          PENDING_USR_TRANSFER_COMPLETE.toString(),
          SepTransactionStatus.COMPLETED.toString(),
          PENDING_EXTERNAL.toString(),
          PENDING_ANCHOR.toString(),
          PENDING_USER.toString());

  public static String constructMoreInfoUrl(
      JwtService jwtService, Sep24Config sep24Config, Sep24Transaction txn)
      throws URISyntaxException, MalformedURLException {

    JwtToken token =
        JwtToken.of(
            "moreInfoUrl",
            txn.getStellarAccount(),
            Instant.now().getEpochSecond(),
            Instant.now().getEpochSecond() + sep24Config.getInteractiveJwtExpiration(),
            txn.getTransactionId(),
            txn.getDomainClient());

    URI uri = new URI(sep24Config.getInteractiveUrl());

    URIBuilder builder =
        new URIBuilder()
            .setScheme(uri.getScheme())
            .setHost(uri.getHost())
            .setPort(uri.getPort())
            .setPath("transaction-status")
            .addParameter("transaction_id", txn.getTransactionId())
            .addParameter("token", jwtService.encode(token));

    return builder.build().toURL().toString();
  }

  public static TransactionResponse fromDepositTxn(
      Gson gson,
      JwtService jwtService,
      Sep24Config sep24Config,
      Sep24Transaction txn,
      boolean allowMoreInfoUrl)
      throws MalformedURLException, URISyntaxException {

    String strJson = gson.toJson(txn);
    DepositTransactionResponse txnR = gson.fromJson(strJson, DepositTransactionResponse.class);

    txnR.setStartedAt(
        (txn.getStartedAt() == null) ? null : DateUtil.toISO8601UTC(txn.getStartedAt()));
    txnR.setCompletedAt(
        (txn.getCompletedAt() == null) ? null : DateUtil.toISO8601UTC(txn.getCompletedAt()));
    txnR.setId(txn.getTransactionId());

    txnR.setDepositMemo(txn.getMemo());
    txnR.setDepositMemoType(txn.getMemoType());

    if (allowMoreInfoUrl && needsMoreInfoUrlDeposit.contains(txn.getStatus())) {
      txnR.setMoreInfoUrl(constructMoreInfoUrl(jwtService, sep24Config, txn));
    } else {
      txnR.setMoreInfoUrl(null);
    }

    return txnR;
  }

  public static WithdrawTransactionResponse fromWithdrawTxn(
      Gson gson,
      JwtService jwtService,
      Sep24Config sep24Config,
      Sep24Transaction txn,
      boolean allowMoreInfoUrl)
      throws MalformedURLException, URISyntaxException {

    String strJson = gson.toJson(txn);
    WithdrawTransactionResponse txnR = gson.fromJson(strJson, WithdrawTransactionResponse.class);

    txnR.setStartedAt(
        (txn.getStartedAt() == null) ? null : DateUtil.toISO8601UTC(txn.getStartedAt()));
    txnR.setCompletedAt(
        (txn.getCompletedAt() == null) ? null : DateUtil.toISO8601UTC(txn.getCompletedAt()));
    txnR.setId(txn.getTransactionId());
    txnR.setFrom(txn.getFromAccount());
    txnR.setTo(txn.getReceivingAnchorAccount());

    txnR.setWithdrawMemo(txn.getMemo());
    txnR.setWithdrawMemoType(txn.getMemoType());
    txnR.setWithdrawAnchorAccount(txn.getReceivingAnchorAccount());

    if (allowMoreInfoUrl && needsMoreInfoUrlWithdraw.contains(txn.getStatus())) {
      txnR.setMoreInfoUrl(constructMoreInfoUrl(jwtService, sep24Config, txn));
    } else {
      txnR.setMoreInfoUrl(null);
    }

    return txnR;
  }
}
