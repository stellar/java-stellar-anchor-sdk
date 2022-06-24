package org.stellar.anchor.sep10;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class JwtToken {
  public JwtToken() {}

  String iss; // Issuer
  String sub; // Subject          // Stellar Account
  long iat; // Issued At
  long exp; // Expiration Time
  String jti; // JWT ID           // Stellar Transaction ID
  // String aud;   // Audience
  // long nbf;     // Not Before

  @SerializedName(value = "client_domain")
  String clientDomain;

  String account;

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
    if (sub != null) {
      String[] subs = sub.split(":", 2);
      if (subs.length == 2) {
        token.account = subs[0];
        token.accountMemo = subs[1];
      } else {
        // TODO: test for muxed account. If the account is muxed, update both fields `account` and
        // `muxedAccount`.
        token.account = sub;
        token.accountMemo = null;
      }
    }
    return token;
  }

  public static JwtToken of(String iss, long iat, long exp) {
    JwtToken token = new JwtToken();
    token.iss = iss;
    token.iat = iat;
    token.exp = exp;
    return token;
  }
}
