package org.stellar.anchor.sep10;

import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.infoF;

import java.io.IOException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.util.Sep1Helper;
import org.stellar.sdk.FormatException;
import org.stellar.sdk.KeyPair;

public class Sep10Helper {

  /**
   * Fetch SIGNING_KEY from clint_domain by reading the stellar.toml content.
   *
   * @param clientDomain The client's domain. E.g. lobstr.co.
   * @return The SIGNING_KEY presented in client's TOML file.
   * @throws SepException if SIGNING_KEY not present or error happens
   */
  public static String fetchSigningKeyFromClientDomain(String clientDomain) throws SepException {
    String clientSigningKey = "";
    String url = "https://" + clientDomain + "/.well-known/stellar.toml";
    try {
      debugF("Fetching {}", url);
      Sep1Helper.TomlContent toml = Sep1Helper.readToml(url);
      clientSigningKey = toml.getString("SIGNING_KEY");
      if (clientSigningKey == null) {
        infoF("SIGNING_KEY not present in 'client_domain' TOML.");
        throw new SepException("SIGNING_KEY not present in 'client_domain' TOML");
      }

      // client key validation
      debugF("Validating client_domain signing key: {}", clientSigningKey);
      KeyPair.fromAccountId(clientSigningKey);
      return clientSigningKey;
    } catch (IllegalArgumentException | FormatException ex) {
      infoF("SIGNING_KEY {} is not a valid Stellar account Id.", clientSigningKey);
      throw new SepException(
          String.format("SIGNING_KEY %s is not a valid Stellar account Id.", clientSigningKey));
    } catch (IOException ioex) {
      infoF("Unable to read from {}", url);
      throw new SepException(String.format("Unable to read from %s", url), ioex);
    }
  }
}
