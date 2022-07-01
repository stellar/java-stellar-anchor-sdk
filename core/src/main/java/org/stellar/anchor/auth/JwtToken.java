package org.stellar.anchor.auth;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.stellar.sdk.AccountConverter;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.xdr.MuxedAccount;

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

  Long muxedAccountId;

  public Long getMuxedAccountId() {
    return this.muxedAccountId;
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

    // Parse account & memo or muxedAccount & muxedAccountId:
    if (sub != null) {
      String[] subs = sub.split(":", 2);
      if (subs.length == 2) {
        token.account = subs[0];
        token.accountMemo = subs[1];
      } else {
        token.account = sub;
        token.accountMemo = null;

        try {
          MuxedAccount maybeMuxedAccount = AccountConverter.enableMuxed().encode(sub);
          MuxedAccount.MuxedAccountMed25519 muxedAccount = maybeMuxedAccount.getMed25519();
          if (muxedAccount == null) {
            return token;
          }

          token.muxedAccount = sub;
          byte[] pubKeyBytes = muxedAccount.getEd25519().getUint256();
          token.account = KeyPair.fromPublicKey(pubKeyBytes).getAccountId();
          token.muxedAccountId = muxedAccount.getId().getUint64();
        } catch (Exception ignored) {
        }
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
