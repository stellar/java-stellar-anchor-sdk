package org.stellar.anchor.sep10;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
@SuppressWarnings("unused")
public class JwtToken {
  public static final String REQUESTED_ACCOUNT = "requested_account";

  public JwtToken() {}

  String iss;
  String sub;
  long iat;
  long exp;
  String jti;
  String account;

  @SerializedName(value = "client_domain")
  String clientDomain;

  @SerializedName(value = "account_memo")
  String accountMemo;

  @SerializedName(value = "muxed_account")
  String muxedAccount;

  @SerializedName(value = REQUESTED_ACCOUNT)
  @Nullable
  String requestedAccount;

  public String getTransactionId() {
    return this.jti;
  }

  public String getIssuer() {
    return this.iss;
  }

  public long getIssuedAt() {
    return this.iat;
  }

  public long getExpiresAt() {
    return this.exp;
  }

  public static JwtToken of(
      String iss,
      String sub,
      long iat,
      long exp,
      String jti,
      String clientDomain,
      String requestedAccount) {
    JwtToken token = new JwtToken();
    token.iss = iss;
    token.sub = sub;
    token.iat = iat;
    token.exp = exp;
    token.jti = jti;
    token.clientDomain = clientDomain;
    token.requestedAccount = requestedAccount;

    String[] subs = sub.split(":");
    if (subs.length == 2) {
      token.account = subs[0];
      token.accountMemo = subs[1];
    } else {
      token.account = sub;
      token.accountMemo = null;
    }
    return token;
  }
}
