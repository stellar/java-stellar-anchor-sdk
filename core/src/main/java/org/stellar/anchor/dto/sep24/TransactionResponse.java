package org.stellar.anchor.dto.sep24;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import lombok.Data;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.model.Sep24Transaction;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.JwtToken;

@Data
public class TransactionResponse {
  String id;

  String kind;

  String status;

  @SerializedName("status_eta")
  Integer status_eta;

  @SerializedName("more_info_url")
  String moreInfoUrl = "";

  @SerializedName("amount_in")
  String amountIn = "0";

  @SerializedName("amount_in_asset")
  String amountInAsset;

  @SerializedName("amount_out")
  String amountOut = "0";

  @SerializedName("amount_out_asset")
  String amountOutAsset;

  @SerializedName("amount_fee")
  String amountFee = "0";

  @SerializedName("amount_fee_asset")
  String amountFeeAsset;

  @SerializedName("started_at")
  String startedAt = "";

  @SerializedName("completed_at")
  String completedAt = "";

  @SerializedName("stellar_transaction_id")
  String stellarTransactionId = "";

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  String message;

  Boolean refunded = false;

  String from = "";

  String to = "";

  @SerializedName("account_memo")
  String accountMemo;

  @SerializedName("muxed_account")
  String muxedAccount;

  static final Gson gson = new Gson();

  static String constructMoreInfoUrl(
      JwtService jwtService, Sep24Config sep24Config, Sep24Transaction txn, String lang)
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

    if (lang != null) {
      builder.addParameter("lang", lang);
    }

    return builder.build().toURL().toString();
  }
}
