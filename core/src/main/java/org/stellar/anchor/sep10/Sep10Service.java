package org.stellar.anchor.sep10;

import static java.lang.String.*;
import static org.stellar.anchor.util.Log.*;
import static org.stellar.anchor.util.MetricConstants.SEP10_CHALLENGE_CREATED;
import static org.stellar.anchor.util.MetricConstants.SEP10_CHALLENGE_VALIDATED;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import java.io.IOException;
import java.time.Instant;
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
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.StringHelper;
import org.stellar.sdk.*;
import org.stellar.sdk.Sep10Challenge.ChallengeTransaction;
import org.stellar.sdk.requests.ErrorResponse;
import org.stellar.sdk.responses.AccountResponse;

/** The Sep-10 protocol service. */
public class Sep10Service implements ISep10Service {
  final AppConfig appConfig;
  final SecretConfig secretConfig;
  final Sep10Config sep10Config;
  final Horizon horizon;
  final JwtService jwtService;
  final String serverAccountId;
  final Counter sep10ChallengeCreatedCounter = Metrics.counter(SEP10_CHALLENGE_CREATED);
  final Counter sep10ChallengeValidatedCounter = Metrics.counter(SEP10_CHALLENGE_VALIDATED);

  public Sep10Service(
      AppConfig appConfig,
      SecretConfig secretConfig,
      Sep10Config sep10Config,
      Horizon horizon,
      JwtService jwtService) {
    debug("appConfig:", appConfig);
    debug("sep10Config:", sep10Config);
    this.appConfig = appConfig;
    this.secretConfig = secretConfig;
    this.sep10Config = sep10Config;
    this.horizon = horizon;
    this.jwtService = jwtService;
    this.serverAccountId =
        KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed()).getAccountId();
    Log.info("Sep10Service initialized.");
  }

  public ChallengeResponse createChallenge(ChallengeRequest request) throws SepException {
    info("Creating SEP-10 challenge.");

    // pre validation to be defined by the anchor
    preChallengeRequestValidation(request);
    // Basic request validation, including null, account format, etc
    validateChallengeRequestFormat(request);
    // validate client_attribution, custodial/non-custodial wallet, etc
    validateChallengeRequestClient(request);
    // Validate the validity of the memo
    Memo memo = validateChallengeRequestMemo(request);
    // post validations to be defined by the anchor
    postChallengeRequestValidation(request);
    // increment counter
    incrementChallengeRequestCreatedCounter();
    // Create the challenge
    return createChallengeResponse(request, memo);
  }

  public ValidationResponse validateChallenge(ValidationRequest request)
      throws IOException, InvalidSep10ChallengeException, SepValidationException {
    info("Validating SEP-10 challenge.");

    ChallengeTransaction challenge = parseChallenge(request);
    // pre validation to be defined by the anchor
    preValidateRequestValidation(request, challenge);

    String homeDomain = validateChallengeTransactionHomeDomain(challenge);

    // fetch the client domain from the transaction
    String clientDomain = fetchClientDomain(challenge);
    // fetch the account response from the horizon
    AccountResponse account = fetchAccount(request, challenge, clientDomain);

    if (account == null) {
      // The account does not exist from Horizon, using the client's master key to verify.
      return ValidationResponse.of(generateSep10Jwt(challenge, clientDomain, homeDomain));
    }
    // Since the account exists, we should check the signers and the client domain
    validateChallengeRequest(request, account, clientDomain);
    // increment counter
    incrementValidationRequestValidatedCounter();
    // Generate the JWT token
    return ValidationResponse.of(generateSep10Jwt(challenge, clientDomain, homeDomain));
  }

  @Override
  public String validateChallengeTransactionHomeDomain(ChallengeTransaction challenge)
      throws SepValidationException {
    String homeDomain;
    try {
      homeDomain = getMatchedHomeDomain(challenge);
    } catch (Throwable e) {
      throw new SepValidationException("Invalid challenge transaction.");
    }

    if (!Objects.equals(sep10Config.getHomeDomain(), homeDomain)) {
      throw new SepValidationException(format("Invalid home_domain. %s", homeDomain));
    }

    return homeDomain;
  }

  private String getMatchedHomeDomain(ChallengeTransaction challenge)
      throws SepValidationException {
    String homeDomain;
    try {
      ManageDataOperation operation =
          (ManageDataOperation) challenge.getTransaction().getOperations()[0];
      homeDomain = operation.getName().split(" ")[0];
    } catch (Throwable e) {
      throw new SepValidationException("Invalid challenge transaction.");
    }

    if (!Objects.equals(sep10Config.getHomeDomain(), homeDomain)) {
      throw new SepValidationException(format("Invalid home_domain. %s", homeDomain));
    }

    return homeDomain;
  }

  @Override
  public ChallengeResponse createChallengeResponse(ChallengeRequest request, Memo memo)
      throws SepException {
    try {
      String clientSigningKey = null;
      if (!StringHelper.isEmpty(request.getClientDomain())) {
        debugF("Fetching SIGNING_KEY from client_domain: {}", request.getClientDomain());
        clientSigningKey = fetchSigningKeyFromClientDomain(request.getClientDomain());
        debugF("SIGNING_KEY from client_domain fetched: {}", clientSigningKey);
      }

      // create challenge transaction
      Transaction txn = newChallenge(request, clientSigningKey, memo);

      // Convert the challenge to response
      trace("SEP-10 challenge txn:", txn);
      ChallengeResponse challengeResponse =
          ChallengeResponse.of(txn.toEnvelopeXdrBase64(), appConfig.getStellarNetworkPassphrase());
      trace("challengeResponse:", challengeResponse);
      return challengeResponse;
    } catch (InvalidSep10ChallengeException ex) {
      warnEx(ex);
      throw new SepException("Failed to create the sep-10 challenge.", ex);
    }
  }

  Transaction newChallenge(ChallengeRequest request, String clientSigningKey, Memo memo)
      throws InvalidSep10ChallengeException {

    KeyPair signer = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());
    long now = Instant.now().getEpochSecond();

    return Sep10ChallengeWrapper.instance()
        .newChallenge(
            signer,
            new Network(appConfig.getStellarNetworkPassphrase()),
            request.getAccount(),
            request.getHomeDomain(),
            sep10Config.getWebAuthDomain(),
            new TimeBounds(now, now + sep10Config.getAuthTimeout()),
            (request.getClientDomain() == null) ? "" : request.getClientDomain(),
            (clientSigningKey == null) ? "" : clientSigningKey,
            memo);
  }

  @Override
  public Memo validateChallengeRequestMemo(ChallengeRequest request) throws SepException {
    // Validate memo. It should be 64-bit positive integer if not null.
    try {
      if (request.getMemo() != null) {
        long memoLong = Long.parseUnsignedLong(request.getMemo());
        if (memoLong <= 0) {
          infoF("Invalid memo value: {}", request.getMemo());
          throw new SepValidationException(format("Invalid memo value: %s", request.getMemo()));
        }
        return new MemoId(memoLong);
      } else {
        return null;
      }
    } catch (NumberFormatException e) {
      infoF("invalid memo format: {}. Only MEMO_ID is supported", request.getMemo());
      throw new SepValidationException(format("Invalid memo format: %s", request.getMemo()));
    }
  }

  @Override
  public void validateChallengeRequestClient(ChallengeRequest request) throws SepException {
    boolean custodialWallet = false;
    if (sep10Config.getKnownCustodialAccountList() != null) {
      custodialWallet =
          sep10Config.getKnownCustodialAccountList().contains(request.getAccount().trim());
    }

    if (custodialWallet && request.getClientDomain() != null) {
      throw new SepValidationException(
          "client_domain must not be specified if the account is an custodial-wallet account");
    }

    if (!custodialWallet && sep10Config.isClientAttributionRequired()) {
      if (request.getClientDomain() == null) {
        info("client_domain is required but not provided");
        throw new SepValidationException("client_domain is required");
      }

      List<String> allowList = sep10Config.getAllowedClientDomains();
      if (!allowList.contains(request.getClientDomain())) {
        infoF(
            "client_domain provided ({}) is not in the configured allow list",
            request.getClientDomain());
        throw new SepNotAuthorizedException("unable to process");
      }
    }
  }

  @Override
  public void validateChallengeRequestFormat(ChallengeRequest request) throws SepException {
    // Make sure the request is requested for the correct home domain
    validateHomeDomain(request);
    // Validate request account format
    validateAccountFormat(request);
  }

  @Override
  public void preChallengeRequestValidation(ChallengeRequest request) {
    // NOOP. To be overridden.
  }

  @Override
  public void postChallengeRequestValidation(ChallengeRequest request) {
    // NOOP. To be overridden.
  }

  @Override
  public void incrementChallengeRequestCreatedCounter() {
    sep10ChallengeCreatedCounter.increment();
  }

  @Override
  public void preValidateRequestValidation(
      ValidationRequest request, ChallengeTransaction challenge) {
    // NOOP. To be overridden.
  }

  @Override
  public void incrementValidationRequestValidatedCounter() {
    sep10ChallengeValidatedCounter.increment();
  }

  String fetchSigningKeyFromClientDomain(String clientDomain) throws SepException {
    return Sep10Helper.fetchSigningKeyFromClientDomain(clientDomain);
  }

  void validateHomeDomain(ChallengeRequest request) throws SepValidationException {
    String homeDomain = request.getHomeDomain();
    if (homeDomain == null) {
      debugF("home_domain is not specified. Will use the default: {}", sep10Config.getHomeDomain());
      request.setHomeDomain(sep10Config.getHomeDomain());
    } else if (!homeDomain.equalsIgnoreCase(sep10Config.getHomeDomain())) {
      infoF("Bad home_domain: {}", homeDomain);
      throw new SepValidationException(format("home_domain [%s] is not supported.", homeDomain));
    }
  }

  void validateAccountFormat(ChallengeRequest request) throws SepException {
    // Validate account
    try {
      KeyPair.fromAccountId(request.getAccount());
    } catch (Exception ex) {
      infoF("client wallet account ({}) is invalid", request.getAccount());
      throw new SepValidationException("Invalid account.");
    }
  }

  void validateChallengeRequest(
      ValidationRequest request, AccountResponse account, String clientDomain)
      throws InvalidSep10ChallengeException, IOException {
    // fetch the signers from the transaction
    Set<Sep10Challenge.Signer> signers = fetchSigners(account);
    // the signatures must be greater than the medium threshold of the account.
    int threshold = account.getThresholds().getMedThreshold();

    infoF(
        "Verifying challenge threshold. server_account={}, client_domain={}, threshold={}, signers={}",
        shorter(serverAccountId),
        clientDomain,
        threshold,
        signers.size());
    Sep10ChallengeWrapper.instance()
        .verifyChallengeTransactionThreshold(
            request.getTransaction(),
            serverAccountId,
            new Network(appConfig.getStellarNetworkPassphrase()),
            sep10Config.getHomeDomain(),
            sep10Config.getWebAuthDomain(),
            threshold,
            signers);
  }

  Set<Sep10Challenge.Signer> fetchSigners(AccountResponse account) {
    // Find the signers of the client account.
    return Arrays.stream(account.getSigners())
        .filter(as -> as.getType().equals("ed25519_public_key"))
        .map(as -> new Sep10Challenge.Signer(as.getKey(), as.getWeight()))
        .collect(Collectors.toSet());
  }

  AccountResponse fetchAccount(
      ValidationRequest request, ChallengeTransaction challenge, String clientDomain)
      throws InvalidSep10ChallengeException, IOException {
    // Check the client's account
    AccountResponse account;
    try {
      infoF("Checking if {} exists in the Stellar network", challenge.getClientAccountId());
      account = horizon.getServer().accounts().account(challenge.getClientAccountId());
      traceF("challenge account: {}", account);
      sep10ChallengeValidatedCounter.increment();
      return account;
    } catch (ErrorResponse | IOException ex) {
      infoF("Account {} does not exist in the Stellar Network");
      // account not found
      // The client account does not exist, using the client's master key to verify.
      Set<String> signers = new HashSet<>();
      signers.add(challenge.getClientAccountId());

      infoF(
          "Verifying challenge threshold. server_account={}, client_domain={}, signers={}",
          shorter(serverAccountId),
          clientDomain,
          signers.size());

      if ((clientDomain != null && challenge.getTransaction().getSignatures().size() != 3)
          || (clientDomain == null && challenge.getTransaction().getSignatures().size() != 2)) {
        infoF(
            "Invalid SEP 10 challenge exception, there is more than one client signer on challenge transaction for an account that doesn't exist. client_domain={}, account_id={}",
            clientDomain,
            challenge.getClientAccountId());
        throw new InvalidSep10ChallengeException(
            "There is more than one client signer on challenge transaction for an account that doesn't exist");
      }

      debug("Calling Sep10Challenge.verifyChallengeTransactionSigners");
      Sep10ChallengeWrapper.instance()
          .verifyChallengeTransactionSigners(
              request.getTransaction(),
              serverAccountId,
              new Network(appConfig.getStellarNetworkPassphrase()),
              sep10Config.getHomeDomain(),
              sep10Config.getWebAuthDomain(),
              signers);

      // increment counter
    }
    sep10ChallengeValidatedCounter.increment();
    return null;
  }

  String fetchClientDomain(ChallengeTransaction challenge) {
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

    return clientDomain;
  }

  ChallengeTransaction parseChallenge(ValidationRequest request)
      throws IOException, InvalidSep10ChallengeException, SepValidationException {

    if (request == null || request.getTransaction() == null) {
      throw new SepValidationException("{transaction} is required.");
    }

    String transaction = request.getTransaction();

    debug("Parse challenge string.");
    ChallengeTransaction challenge =
        Sep10ChallengeWrapper.instance()
            .readChallengeTransaction(
                transaction,
                serverAccountId,
                new Network(appConfig.getStellarNetworkPassphrase()),
                sep10Config.getHomeDomain(),
                sep10Config.getWebAuthDomain());

    debugF(
        "Challenge parsed. account={}, home_domain={}",
        shorter(challenge.getClientAccountId()),
        challenge.getMatchedHomeDomain());

    trace("challenge:", challenge);

    return challenge;
  }

  String generateSep10Jwt(ChallengeTransaction challenge, String clientDomain, String homeDomain) {
    long issuedAt = challenge.getTransaction().getTimeBounds().getMinTime().longValue();
    Memo memo = challenge.getTransaction().getMemo();
    Sep10Jwt sep10Jwt =
        Sep10Jwt.of(
            "https://" + sep10Config.getWebAuthDomain() + "/auth",
            (memo == null || memo instanceof MemoNone)
                ? challenge.getClientAccountId()
                : challenge.getClientAccountId() + ":" + memo,
            issuedAt,
            issuedAt + sep10Config.getJwtTimeout(),
            challenge.getTransaction().hashHex(),
            clientDomain,
            homeDomain);
    debug("jwtToken:", sep10Jwt);
    return jwtService.encode(sep10Jwt);
  }
}

class Sep10ChallengeWrapper {
  static Sep10ChallengeWrapper instance = new Sep10ChallengeWrapper();

  public static Sep10ChallengeWrapper instance() {
    return instance;
  }

  public synchronized Transaction newChallenge(
      KeyPair signer,
      Network network,
      String clientAccountId,
      String domainName,
      String webAuthDomain,
      TimeBounds timebounds,
      String clientDomain,
      String clientSigningKey,
      Memo memo)
      throws InvalidSep10ChallengeException {
    return Sep10Challenge.newChallenge(
        signer,
        network,
        clientAccountId,
        domainName,
        webAuthDomain,
        timebounds,
        clientDomain,
        clientSigningKey,
        memo);
  }

  public synchronized ChallengeTransaction readChallengeTransaction(
      String challengeXdr,
      String serverAccountId,
      Network network,
      String domainName,
      String webAuthDomain)
      throws InvalidSep10ChallengeException, IOException {
    return Sep10Challenge.readChallengeTransaction(
        challengeXdr, serverAccountId, network, domainName, webAuthDomain);
  }

  public synchronized void verifyChallengeTransactionSigners(
      String challengeXdr,
      String serverAccountId,
      Network network,
      String domainName,
      String webAuthDomain,
      Set<String> signers)
      throws InvalidSep10ChallengeException, IOException {
    Sep10Challenge.verifyChallengeTransactionSigners(
        challengeXdr, serverAccountId, network, domainName, webAuthDomain, signers);
  }

  public synchronized void verifyChallengeTransactionThreshold(
      String challengeXdr,
      String serverAccountId,
      Network network,
      String domainName,
      String webAuthDomain,
      int threshold,
      Set<Sep10Challenge.Signer> signers)
      throws InvalidSep10ChallengeException, IOException {
    Sep10Challenge.verifyChallengeTransactionThreshold(
        challengeXdr, serverAccountId, network, domainName, webAuthDomain, threshold, signers);
  }
}
