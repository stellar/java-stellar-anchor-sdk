package org.stellar.anchor.sep10;

import static org.stellar.anchor.util.Log.infoF;
import static org.stellar.anchor.util.Log.shorter;

import com.moandjiezana.toml.Toml;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.dto.sep10.ChallengeRequest;
import org.stellar.anchor.dto.sep10.ChallengeResponse;
import org.stellar.anchor.dto.sep10.ValidationRequest;
import org.stellar.anchor.dto.sep10.ValidationResponse;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.exception.SepValidationException;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.NetUtil;
import org.stellar.sdk.*;
import org.stellar.sdk.requests.ErrorResponse;
import org.stellar.sdk.responses.AccountResponse;

/** The Sep-10 protocol service. */
public class Sep10Service {
  final AppConfig appConfig;
  final Sep10Config sep10Config;
  final Horizon horizon;
  final JwtService jwtService;
  final String serverAccountId;

  public Sep10Service(
      AppConfig appConfig, Sep10Config sep10Config, Horizon horizon, JwtService jwtService) {
    this.appConfig = appConfig;
    this.sep10Config = sep10Config;
    this.horizon = horizon;
    this.jwtService = jwtService;
    this.serverAccountId = KeyPair.fromSecretSeed(sep10Config.getSigningSeed()).getAccountId();
  }

  public ChallengeResponse createChallenge(ChallengeRequest challengeRequest) throws SepException {
    //
    // Validations
    //
    if (challengeRequest.getHomeDomain() == null) {
      challengeRequest.setHomeDomain(sep10Config.getHomeDomain());
    } else if (!sep10Config.getHomeDomain().equalsIgnoreCase(challengeRequest.getHomeDomain())) {
      throw new SepValidationException(
          String.format("home_domain [%s] is not supported.", challengeRequest.getHomeDomain()));
    }

    boolean omnibusWallet =
        sep10Config.getOmnibusAccountList().contains(challengeRequest.getAccount().trim());
    if (omnibusWallet) {
      if (challengeRequest.getClientDomain() != null) {
        throw new SepValidationException(
            "client_domain must not be specified if the account is an omni-wallet account");
      }
    }

    if (!omnibusWallet && sep10Config.isClientAttributionRequired()) {
      if (challengeRequest.getClientDomain() == null) {
        infoF("ALERT: client domain required and not provided");
        throw new SepValidationException("client_domain is required");
      }

      List<String> denyList = sep10Config.getClientAttributionDenyList();
      if (denyList != null
          && denyList.size() > 0
          && denyList.contains(challengeRequest.getClientDomain())) {
        infoF(
            "ALERT: client domain provided is in configured deny list - {} ",
            challengeRequest.getClientDomain());
        throw new SepValidationException("unable to process.");
      }

      List<String> allowList = sep10Config.getClientAttributionAllowList();
      if (allowList != null
          && allowList.size() > 0
          && !allowList.contains(challengeRequest.getClientDomain())) {
        infoF(
            "ALERT: client domain provided is not in configured allow list - {} ",
            challengeRequest.getClientDomain());
        throw new SepValidationException("unable to process");
      }
    }
    // Validate account
    try {
      KeyPair.fromAccountId(challengeRequest.getAccount());
    } catch (Exception ex) {
      infoF("ALERT: client wallet account is invalid - {}", challengeRequest.getAccount());
      throw new SepValidationException("Invalid account.");
    }

    // Validate memo. It should be 64-bit positive integer if not null.
    Memo memo = null;
    try {
      if (challengeRequest.getMemo() != null) {
        long memoLong = Long.parseUnsignedLong(challengeRequest.getMemo());
        if (memoLong <= 0) {
          throw new SepValidationException(
              String.format("Invalid memo value: %s", challengeRequest.getMemo()));
        }
        memo = new MemoId(memoLong);
      }
    } catch (NumberFormatException e) {
      throw new SepValidationException(
          String.format("Invalid memo format: %s", challengeRequest.getMemo()));
    }

    //
    // Create the challenge
    //
    try {
      String clientSigningKey = null;
      if (challengeRequest.getClientDomain() != null) {
        clientSigningKey = getClientAccountId(challengeRequest.getClientDomain());
      }

      KeyPair signer = KeyPair.fromSecretSeed(sep10Config.getSigningSeed());
      long now = System.currentTimeMillis() / 1000L;
      Transaction txn =
          Sep10Challenge.newChallenge(
              signer,
              new Network(appConfig.getStellarNetworkPassphrase()),
              challengeRequest.getAccount(),
              challengeRequest.getHomeDomain(),
              getDomainFromURI(appConfig.getHostUrl()),
              new TimeBounds(now, now + sep10Config.getAuthTimeout()),
              (challengeRequest.getClientDomain() == null)
                  ? ""
                  : challengeRequest.getClientDomain(),
              (clientSigningKey == null) ? "" : clientSigningKey,
              memo);
      // Convert the challenge to response
      return ChallengeResponse.of(
          txn.toEnvelopeXdrBase64(), appConfig.getStellarNetworkPassphrase());
    } catch (URISyntaxException e) {
      throw new SepException(
          String.format("Invalid HOST_URL [%s} is used.", appConfig.getHostUrl()));
    } catch (InvalidSep10ChallengeException ex) {
      throw new SepException("Failed to create the sep-10 challenge.", ex);
    }
  }

  public ValidationResponse validateChallenge(ValidationRequest validationRequest)
      throws IOException,
          InvalidSep10ChallengeException,
          URISyntaxException,
          SepValidationException {
    if (validationRequest == null || validationRequest.getTransaction() == null) {
      throw new SepValidationException("{transaction} is required.");
    }

    Log.debug("Parse challenge string.");
    String challengeXdr = validationRequest.getTransaction();
    Sep10Challenge.ChallengeTransaction challenge =
        Sep10Challenge.readChallengeTransaction(
            challengeXdr,
            serverAccountId,
            new Network(appConfig.getStellarNetworkPassphrase()),
            sep10Config.getHomeDomain(),
            getDomainFromURI(appConfig.getHostUrl()));

    infoF(
        "Challenge parsed. account={}, home_domain={}",
        shorter(challenge.getClientAccountId()),
        challenge.getMatchedHomeDomain());

    String clientDomain = null;
    Operation operation =
        Arrays.stream(challenge.getTransaction().getOperations())
            .filter(
                op ->
                    (op instanceof ManageDataOperation
                        && ((ManageDataOperation) op).getName().equals("client_domain")))
            .findFirst()
            .orElse(null);

    if (operation != null) {
      clientDomain = new String(((ManageDataOperation) operation).getValue());
    }

    // Check the client's account
    AccountResponse account;
    try {
      account = horizon.getServer().accounts().account(challenge.getClientAccountId());
    } catch (ErrorResponse ex) {
      // account not found
      // The client account does not exist, using the client's master key to verify.
      Set<String> signers = new HashSet<>();
      signers.add(challenge.getClientAccountId());

      infoF(
          "Verifying challenge threshold. server_account={}, signers={}",
          shorter(serverAccountId),
          signers.size());

      if ((clientDomain != null && challenge.getTransaction().getSignatures().size() != 3)
          || (clientDomain == null && challenge.getTransaction().getSignatures().size() != 2)) {
        infoF(
            "ALERT: Invalid SEP 10 challenge exception, there is more than one client signer on challenge transaction for an account that doesn't exist");
        throw new InvalidSep10ChallengeException(
            "There is more than one client signer on challenge transaction for an account that doesn't exist");
      }

      Sep10Challenge.verifyChallengeTransactionSigners(
          challengeXdr,
          serverAccountId,
          new Network(appConfig.getStellarNetworkPassphrase()),
          sep10Config.getHomeDomain(),
          getDomainFromURI(appConfig.getHostUrl()),
          signers);

      return ValidationResponse.of(
          generateSep10Jwt(validationRequest.getTransaction(), clientDomain));
    }

    // Find the signers of the client account.
    Set<Sep10Challenge.Signer> signers =
        Arrays.stream(account.getSigners())
            .filter(as -> as.getType().equals("ed25519_public_key"))
            .map(as -> new Sep10Challenge.Signer(as.getKey(), as.getWeight()))
            .collect(Collectors.toSet());

    // the signatures must be greater than the medium threshold of the account.
    int threshold = account.getThresholds().getMedThreshold();
    infoF(
        "Verifying challenge threshold. server_account={}, threshold={}, signers={}",
        shorter(serverAccountId),
        threshold,
        signers.size());

    Sep10Challenge.verifyChallengeTransactionThreshold(
        challengeXdr,
        serverAccountId,
        new Network(appConfig.getStellarNetworkPassphrase()),
        sep10Config.getHomeDomain(),
        getDomainFromURI(appConfig.getHostUrl()),
        threshold,
        signers);

    return ValidationResponse.of(
        generateSep10Jwt(validationRequest.getTransaction(), clientDomain));
  }

  String getClientAccountId(String clientDomain) throws SepException {
    String clientSigningKey = "";
    String url = "https://" + clientDomain + "/.well-known/stellar.toml";
    try {
      String tomlValue = NetUtil.fetch(url);
      Log.debug("Fetched client_domain's stellar.toml.", tomlValue);

      Toml toml = new Toml().read(tomlValue);
      clientSigningKey = toml.getString("SIGNING_KEY");
      if (clientSigningKey == null) {
        throw new SepException("SIGNING_KEY not present in 'client_domain' TOML");
      }

      // client key validation
      KeyPair.fromAccountId(clientSigningKey);
      return clientSigningKey;
    } catch (IllegalArgumentException | FormatException ex) {
      throw new SepException(
          String.format("SIGNING_KEY %s is not a valid Stellar account Id.", clientSigningKey));
    } catch (IOException ioex) {
      throw new SepException(String.format("Unable to read from %s", url), ioex);
    }
  }

  String generateSep10Jwt(String challengeXdr, String clientDomain)
      throws InvalidSep10ChallengeException, IOException, URISyntaxException {
    Sep10Challenge.ChallengeTransaction challenge =
        Sep10Challenge.readChallengeTransaction(
            challengeXdr,
            serverAccountId,
            new Network(appConfig.getStellarNetworkPassphrase()),
            sep10Config.getHomeDomain(),
            getDomainFromURI(appConfig.getHostUrl()));
    long issuedAt = challenge.getTransaction().getTimeBounds().getMinTime();
    Memo memo = challenge.getTransaction().getMemo();
    JwtToken jwtToken =
        JwtToken.of(
            appConfig.getHostUrl() + "/auth",
            (memo == null || memo instanceof MemoNone)
                ? challenge.getClientAccountId()
                : challenge.getClientAccountId() + ":" + memo,
            issuedAt,
            issuedAt + sep10Config.getJwtTimeout(),
            challenge.getTransaction().hashHex(),
            clientDomain);
    return jwtService.encode(jwtToken);
  }

  String getDomainFromURI(String strUri) throws URISyntaxException {
    URI uri = new URI(strUri);
    if (uri.getPort() < 0) {
      return uri.getHost();
    }
    return uri.getHost() + ":" + uri.getPort();
  }
}
