package org.stellar.anchor.sep10;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
@SuppressWarnings("unused")
public class JwtToken {
  public JwtToken() {}

  String iss;
  String sub;
  long iat;
  long exp;
  String jti;

  @SerializedName(value = "client_domain")
  String clientDomain;

  @SerializedName(value = "account_memo")
  String accountMemo;

  @SerializedName(value = "muxed_account")
  String muxedAccount;

  public String getAccount() {
    return this.sub;
  }

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
      String iss, String sub, long iat, long exp, String jti, String clientDomain) {
    JwtToken token = new JwtToken();
    token.iss = iss;
    token.sub = sub;
    token.iat = iat;
    token.exp = exp;
    token.jti = jti;
    token.clientDomain = clientDomain;
    return token;
  }
}
