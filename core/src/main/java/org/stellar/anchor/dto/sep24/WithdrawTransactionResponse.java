package org.stellar.anchor.dto.sep24;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.model.Sep24Transaction;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.util.DateUtil;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.stellar.anchor.model.TransactionStatus.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class WithdrawTransactionResponse extends TransactionResponse {
  @SerializedName("withdraw_anchor_account")
  String withdrawAnchorAccount;

  @SerializedName("withdraw_memo")
  String withdrawMemo;

  @SerializedName("withdraw_memo_type")
  String withdrawMemoType;

  public static WithdrawTransactionResponse of(
      JwtService jwtService,
      Sep24Config sep24Config,
      Sep24Transaction txn,
      boolean allowMoreInfoUrl)
      throws MalformedURLException, URISyntaxException {
    String strJson = gson.toJson(txn);
    WithdrawTransactionResponse txnR = gson.fromJson(strJson, WithdrawTransactionResponse.class);

    txnR.startedAt =
        (txn.getStartedAt() == null) ? null : DateUtil.toISO8601UTC(txn.getStartedAt());
    txnR.completedAt =
        (txn.getCompletedAt() == null) ? null : DateUtil.toISO8601UTC(txn.getCompletedAt());
    txnR.id = txn.getTransactionId();
    txnR.from = txn.getFromAccount();
    txnR.to = txn.getReceivingAnchorAccount();

    txnR.withdrawMemo = txn.getMemo();
    txnR.withdrawMemoType = txn.getMemoType();
    txnR.withdrawAnchorAccount = txn.getReceivingAnchorAccount();

    if (allowMoreInfoUrl && needsMoreInfoUrlWithdraw.contains(txn.getStatus())) {
      txnR.moreInfoUrl = constructMoreInfoUrl(jwtService, sep24Config, txn);
    } else {
      txnR.moreInfoUrl = null;
    }

    return txnR;
  }

  private static List<String> needsMoreInfoUrlWithdraw =
      Arrays.asList(
          PENDING_USR_TRANSFER_START.toString(),
          PENDING_USR_TRANSFER_COMPLETE.toString(),
          COMPLETED.toString(),
          PENDING_EXTERNAL.toString(),
          PENDING_ANCHOR.toString(),
          PENDING_USER.toString());
}
