package org.stellar.anchor.sep10;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest;
import org.stellar.anchor.api.sep.sep10c.ChallengeResponse;
import org.stellar.anchor.api.sep.sep10c.ValidationRequest;
import org.stellar.anchor.api.sep.sep10c.ValidationResponse;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.rpc.RpcClient;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;
import org.stellar.sdk.*;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.responses.sorobanrpc.SimulateTransactionResponse;
import org.stellar.sdk.scval.Scv;
import org.stellar.sdk.xdr.*;

@AllArgsConstructor
public class Sep10CService {
  private final AppConfig appConfig;
  private final SecretConfig secretConfig;
  private final Sep10Config sep10Config;
  private final JwtService jwtService;
  private final RpcClient rpcClient;

  private static final String WEB_AUTH_VERIFY_FN = "web_auth_verify";

  /**
   * Creates a challenge for the client to sign.
   *
   * @param challengeRequest The challenge request.
   * @return The challenge response.
   */
  public ChallengeResponse createChallenge(ChallengeRequest challengeRequest) throws SepException {
    Log.debugF(
        "Creating challenge for request {}", GsonUtils.getInstance().toJson(challengeRequest));

    Transaction transaction = buildUnsignedTransaction(challengeRequest);
    SimulateTransactionResponse simulateTransactionResponse =
        rpcClient.simulateTransaction(transaction);

    Log.debugF("Simulate transaction response: {}", simulateTransactionResponse);

    SorobanAuthorizationEntry unsignedAuthEntry =
        extractAuthorizationEntry(simulateTransactionResponse);

    return buildChallenge(unsignedAuthEntry);
  }

  private Transaction buildUnsignedTransaction(ChallengeRequest challengeRequest) {
    KeyPair keyPair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());
    TransactionBuilderAccount source = rpcClient.getAccount(keyPair.getAccountId());

    SCVal[] args = buildArgsFromRequest(challengeRequest);
    InvokeHostFunctionOperation operation =
        InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                sep10Config.getWebAuthContractId(), WEB_AUTH_VERIFY_FN, Arrays.asList(args))
            .sourceAccount(source.getAccountId())
            .build();

    return new TransactionBuilder(source, new Network(appConfig.getStellarNetworkPassphrase()))
        .setBaseFee(Transaction.MIN_BASE_FEE)
        .addOperation(operation)
        .setTimeout(300)
        .build();
  }

  private SCVal[] buildArgsFromRequest(ChallengeRequest challengeRequest) {
    return new SCVal[] {
      arg(challengeRequest.getAddress()),
      arg(challengeRequest.getMemo()),
      arg(challengeRequest.getHomeDomain()),
      arg(sep10Config.getWebAuthDomain()),
      arg(challengeRequest.getClientDomain()),
      arg(null) // TODO: client domain verification is not supported
    };
  }

  private SCVal arg(String value) {
    return value == null
        ? new SCVal.Builder().discriminant(SCValType.SCV_VOID).build()
        : new SCVal.Builder()
            .discriminant(SCValType.SCV_STRING)
            .str(Scv.toString(value).getStr())
            .build();
  }

  private SorobanAuthorizationEntry extractAuthorizationEntry(SimulateTransactionResponse response)
      throws SepException {
    try {
      return SorobanAuthorizationEntry.fromXdrBase64(response.getResults().get(0).getAuth().get(0));
    } catch (IOException e) {
      throw new SepException("Failed to decode authorization entry", e);
    }
  }

  private ChallengeResponse buildChallenge(SorobanAuthorizationEntry authorizationEntry)
      throws SepException {
    try {
      return ChallengeResponse.builder()
          .authorizationEntry(authorizationEntry.toXdrBase64())
          .serverSignature(signAuthorizationEntry(authorizationEntry))
          .build();
    } catch (IOException e) {
      throw new SepException("Unable to sign authorization entry", e);
    }
  }

  /**
   * Validates the challenge signed by the client.
   *
   * @param validationRequest The validation request.
   * @return The validation response.
   */
  public ValidationResponse validateChallenge(ValidationRequest validationRequest)
      throws SepException {
    SorobanAuthorizationEntry authorizationEntry =
        extractAuthorizationEntry(validationRequest.getAuthorizationEntry());

    verifyServerSignature(authorizationEntry, validationRequest.getServerSignature());

    // Client credentials is always the first element in the array
    // until we figure out how to do client domain verification
    SorobanCredentials clientCredentials =
        decodeClientCredentials(validationRequest.getCredentials()[0]);

    Transaction transaction = buildSignedTransaction(authorizationEntry, clientCredentials);

    SimulateTransactionResponse response = rpcClient.simulateTransaction(transaction);
    if (response.getError() != null) {
      throw new SepValidationException("Error validating credentials: " + response.getError());
    }
    Log.debugF("Credentials successfully validated: {}", response);

    return ValidationResponse.builder().token(generateJwt(authorizationEntry)).build();
  }

  private SorobanAuthorizationEntry extractAuthorizationEntry(String authorizationEntry)
      throws SepException {
    try {
      return SorobanAuthorizationEntry.fromXdrBase64(authorizationEntry);
    } catch (IOException e) {
      throw new SepValidationException("Failed to decode authorization entry", e);
    }
  }

  private void verifyServerSignature(
      SorobanAuthorizationEntry authorizationEntry, String serverSignature) throws SepException {
    String expectedSignature = signAuthorizationEntry(authorizationEntry);
    if (!expectedSignature.equals(serverSignature)) {
      throw new SepValidationException("Server signature is invalid");
    }
  }

  private SorobanCredentials decodeClientCredentials(String credentials) throws SepException {
    try {
      return SorobanCredentials.fromXdrBase64(credentials);
    } catch (IOException e) {
      throw new SepException("Failed to decode client credentials", e);
    }
  }

  private Transaction buildSignedTransaction(
      SorobanAuthorizationEntry authorizationEntry, SorobanCredentials clientCredentials)
      throws SepException {
    KeyPair keyPair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());
    TransactionBuilderAccount source = rpcClient.getAccount(keyPair.getAccountId());

    SorobanAuthorizationEntry clientSignedAuthEntry =
        attachCredentials(authorizationEntry, clientCredentials);

    SCVal[] parameters =
        authorizationEntry.getRootInvocation().getFunction().getContractFn().getArgs();
    InvokeHostFunctionOperation operation =
        InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                sep10Config.getWebAuthContractId(), WEB_AUTH_VERIFY_FN, Arrays.asList(parameters))
            .sourceAccount(source.getAccountId())
            .auth(Collections.singletonList(clientSignedAuthEntry))
            .build();

    return new TransactionBuilder(source, new Network(appConfig.getStellarNetworkPassphrase()))
        .setBaseFee(Transaction.MIN_BASE_FEE)
        .addOperation(operation)
        .setTimeout(300)
        .build();
  }

  private SorobanAuthorizationEntry attachCredentials(
      SorobanAuthorizationEntry authorizationEntry, SorobanCredentials clientCredentials)
      throws SepException {
    try {
      SorobanAuthorizationEntry authorizationEntryClone =
          SorobanAuthorizationEntry.fromXdrBase64(authorizationEntry.toXdrBase64());
      authorizationEntryClone.setCredentials(clientCredentials);
      Log.debugF("Client signed auth entry: {}", authorizationEntryClone.toXdrBase64());
      return authorizationEntryClone;
    } catch (IOException e) {
      throw new SepException("Failed to decode challenge authorization entry", e);
    }
  }

  private String generateJwt(SorobanAuthorizationEntry authorizationEntry) throws SepException {
    SorobanAuthorizedInvocation invocation = authorizationEntry.getRootInvocation();

    long issuedAt = Instant.now().getEpochSecond();
    String address = getFromContractArgs(invocation, "address").orElse(null);
    String memo = getFromContractArgs(invocation, "memo").orElse(null);
    String clientDomain = getFromContractArgs(invocation, "client_domain").orElse(null);
    String homeDomain = getFromContractArgs(invocation, "home_domain").orElse(null);

    String authUrl = "https://" + sep10Config.getWebAuthDomain() + "/c/auth";
    String hashHex;
    try {
      hashHex = Util.bytesToHex(Util.hash(invocation.toXdrByteArray()));
    } catch (IOException e) {
      throw new SepException("Unable to decode invocation", e);
    }

    Sep10Jwt jwt =
        Sep10Jwt.of(
            authUrl,
            StringUtils.isEmpty(memo) ? address : address + ":" + memo,
            issuedAt,
            issuedAt + sep10Config.getJwtTimeout(),
            hashHex,
            clientDomain,
            homeDomain);

    return jwtService.encode(jwt);
  }

  private Optional<String> getFromContractArgs(SorobanAuthorizedInvocation invocation, String key) {
    switch (key) {
      case "address":
        return Optional.ofNullable(invocation.getFunction().getContractFn().getArgs()[0].getStr())
            .map(SCString::getSCString)
            .map(XdrString::toString);
      case "memo":
        return Optional.ofNullable(invocation.getFunction().getContractFn().getArgs()[1].getStr())
            .map(SCString::getSCString)
            .map(XdrString::toString);
      case "home_domain":
        return Optional.ofNullable(invocation.getFunction().getContractFn().getArgs()[2].getStr())
            .map(SCString::getSCString)
            .map(XdrString::toString);
      case "client_domain":
        return Optional.ofNullable(invocation.getFunction().getContractFn().getArgs()[4].getStr())
            .map(SCString::getSCString)
            .map(XdrString::toString);
    }
    return Optional.empty();
  }

  private String signAuthorizationEntry(SorobanAuthorizationEntry entry) throws SepException {
    KeyPair keypair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());

    try {
      byte[] hash = Util.hash(entry.toXdrByteArray());
      byte[] signature = keypair.sign(hash);

      return Util.bytesToHex(signature);
    } catch (IOException e) {
      throw new SepException("Unable to decode authorization entry", e);
    }
  }
}
