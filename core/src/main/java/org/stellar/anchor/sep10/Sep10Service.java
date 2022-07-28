package org.stellar.anchor.sep10;

import static org.stellar.anchor.util.Log.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.SepNotAuthorizedException;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.api.sep.sep10.ChallengeRequest;
import org.stellar.anchor.api.sep.sep10.ChallengeResponse;
import org.stellar.anchor.api.sep.sep10.ValidationRequest;
import org.stellar.anchor.api.sep.sep10.ValidationResponse;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.Sep1Helper;
import org.stellar.anchor.util.Sep1Helper.TomlContent;
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
    debug("appConfig:", appConfig);
    debug("sep10Config:", sep10Config);
    this.appConfig = appConfig;
    this.sep10Config = sep10Config;
    this.horizon = horizon;
    this.jwtService = jwtService;
    this.serverAccountId = KeyPair.fromSecretSeed(sep10Config.getSigningSeed()).getAccountId();
    Log.info("Sep10Service initialized.");
  }

  public ChallengeResponse createChallenge(ChallengeRequest challengeRequest) throws SepException {
    info("Creating challenge");
    //
    // Validations
    //
    if (challengeRequest.getHomeDomain() == null) {
      debugF("home_domain is not specified. Will use the default: {}", sep10Config.getHomeDomain());
      challengeRequest.setHomeDomain(sep10Config.getHomeDomain());
    } else if (!sep10Config.getHomeDomain().equalsIgnoreCase(challengeRequest.getHomeDomain())) {
      infoF("Bad home_domain: {}", challengeRequest.getHomeDomain());
      throw new SepValidationException(
          String.format("home_domain [%s] is not supported.", challengeRequest.getHomeDomain()));
    }

    boolean omnibusWallet = false;
    if (sep10Config.getOmnibusAccountList() != null) {
      omnibusWallet =
          sep10Config.getOmnibusAccountList().contains(challengeRequest.getAccount().trim());
    }

    if (sep10Config.isRequireKnownOmnibusAccount() && !omnibusWallet) {
      // validate that requesting account is allowed access
      infoF("requesting account: {} is not in allow list", challengeRequest.getAccount().trim());
      throw new SepNotAuthorizedException("unable to process");
    }

    if (omnibusWallet) {
      if (challengeRequest.getClientDomain() != null) {
        throw new SepValidationException(
            "client_domain must not be specified if the account is an omni-wallet account");
      }
    }

    if (!omnibusWallet && sep10Config.isClientAttributionRequired()) {
      if (challengeRequest.getClientDomain() == null) {
        info("client_domain is required but not provided");
        throw new SepValidationException("client_domain is required");
      }

      List<String> denyList = sep10Config.getClientAttributionDenyList();
      if (denyList != null
          && denyList.size() > 0
          && denyList.contains(challengeRequest.getClientDomain())) {
        infoF(
            "client_domain({}) provided is in the configured deny list",
            challengeRequest.getClientDomain());
        throw new SepNotAuthorizedException("unable to process.");
      }

      List<String> allowList = sep10Config.getClientAttributionAllowList();
      if (allowList != null
          && allowList.size() > 0
          && !allowList.contains(challengeRequest.getClientDomain())) {
        infoF(
            "client_domain provided ({}) is not in configured allow list",
            challengeRequest.getClientDomain());
        throw new SepNotAuthorizedException("unable to process");
      }
    }
    // Validate account
    try {
      KeyPair.fromAccountId(challengeRequest.getAccount());
    } catch (Exception ex) {
      infoF("client wallet account ({}) is invalid", challengeRequest.getAccount());
      throw new SepValidationException("Invalid account.");
    }

    Memo memo = null;

    // Validate memo. It should be 64-bit positive integer if not null.
    try {
      if (challengeRequest.getMemo() != null) {
        long memoLong = Long.parseUnsignedLong(challengeRequest.getMemo());
        if (memoLong <= 0) {
          infoF("Invalid memo value: {}", challengeRequest.getMemo());
          throw new SepValidationException(
              String.format("Invalid memo value: %s", challengeRequest.getMemo()));
        }
        memo = new MemoId(memoLong);
      }
    } catch (NumberFormatException e) {
      infoF("invalid memo format: {}. Only MEMO_INT is supported", challengeRequest.getMemo());
      throw new SepValidationException(
          String.format("Invalid memo format: %s", challengeRequest.getMemo()));
    }

    //
    // Create the challenge
    //
    try {
      String clientSigningKey = null;
      if (!Objects.toString(challengeRequest.getClientDomain(), "").isEmpty()) {
        debugF("Fetching SIGNING_KEY from client_domain: {}", challengeRequest.getClientDomain());
        clientSigningKey = getClientAccountId(challengeRequest.getClientDomain());
        debugF("SIGNING_KEY from client_domain fetched: {}", clientSigningKey);
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
      trace("SEP-10 challenge txn:", txn);
      ChallengeResponse challengeResponse =
          ChallengeResponse.of(txn.toEnvelopeXdrBase64(), appConfig.getStellarNetworkPassphrase());
      trace("challengeResponse:", challengeResponse);
      return challengeResponse;
    } catch (URISyntaxException e) {
      warnF("Invalid HOST_URL: {}", appConfig.getHostUrl());
      throw new SepException(
          String.format("Invalid HOST_URL [%s} is used.", appConfig.getHostUrl()));
    } catch (InvalidSep10ChallengeException ex) {
      warnEx(ex);
      throw new SepException("Failed to create the sep-10 challenge.", ex);
    }
  }

  public ValidationResponse validateChallenge(ValidationRequest validationRequest)
      throws IOException, InvalidSep10ChallengeException, URISyntaxException,
          SepValidationException {
    if (validationRequest == null || validationRequest.getTransaction() == null) {
      throw new SepValidationException("{transaction} is required.");
    }

    String clientDomain = validateChallenge(validationRequest.getTransaction());
    return ValidationResponse.of(
        generateSep10Jwt(validationRequest.getTransaction(), clientDomain));
  }

  public String validateChallenge(String challengeXdr)
      throws IOException, InvalidSep10ChallengeException, URISyntaxException {
    debug("Parse challenge string.");
    Sep10Challenge.ChallengeTransaction challenge =
        Sep10Challenge.readChallengeTransaction(
            challengeXdr,
            serverAccountId,
            new Network(appConfig.getStellarNetworkPassphrase()),
            sep10Config.getHomeDomain(),
            getDomainFromURI(appConfig.getHostUrl()));

    debugF(
        "Challenge parsed. account={}, home_domain={}",
        shorter(challenge.getClientAccountId()),
        challenge.getMatchedHomeDomain());

    trace("challenge:", challenge);

    String clientDomain = null;
    Operation operation =
        Arrays.stream(challenge.getTransaction().getOperations())
            .filter(
                op ->
                    (op instanceof ManageDataOperation
                        && ((ManageDataOperation) op).getName().equals("client_domain")))
            .findFirst()
            .orElse(null);

    trace("Challenge operation:", operation);
    if (operation != null) {
      clientDomain = new String(((ManageDataOperation) operation).getValue());
    }
    debugF("client_domain: {}", clientDomain);

    // Check the client's account
    AccountResponse account;
    try {
      infoF("Checking if {} exists in the Stellar network", challenge.getClientAccountId());
      account = horizon.getServer().accounts().account(challenge.getClientAccountId());
      traceF("challenge account: {}", account);
    } catch (ErrorResponse ex) {
      infoF("Account {} does not exist in the Stellar Network");
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
            "Invalid SEP 10 challenge exception, there is more than one client signer on challenge transaction for an account that doesn't exist");
        throw new InvalidSep10ChallengeException(
            "There is more than one client signer on challenge transaction for an account that doesn't exist");
      }

      debug("Calling Sep10Challenge.verifyChallengeTransactionSigners");
      Sep10Challenge.verifyChallengeTransactionSigners(
          challengeXdr,
          serverAccountId,
          new Network(appConfig.getStellarNetworkPassphrase()),
          sep10Config.getHomeDomain(),
          getDomainFromURI(appConfig.getHostUrl()),
          signers);

      return clientDomain;
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

    return clientDomain;
  }

  String getClientAccountId(String clientDomain) throws SepException {
    String clientSigningKey = "";
    String url = "https://" + clientDomain + "/.well-known/stellar.toml";
    try {
      debugF("Fetching {}", url);
      TomlContent toml = Sep1Helper.readToml(url);
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

  String generateSep10Jwt(String challengeXdr, String clientDomain)
      throws InvalidSep10ChallengeException, IOException, URISyntaxException {
    infoF("Creating SEP-10 challenge.");
    Sep10Challenge.ChallengeTransaction challenge =
        Sep10Challenge.readChallengeTransaction(
            challengeXdr,
            serverAccountId,
            new Network(appConfig.getStellarNetworkPassphrase()),
            sep10Config.getHomeDomain(),
            getDomainFromURI(appConfig.getHostUrl()));
    debug("challenge:", challenge);
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
    debug("jwtToken:", jwtToken);
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
