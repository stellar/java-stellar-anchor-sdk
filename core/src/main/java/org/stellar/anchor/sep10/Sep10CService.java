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

    // Simulate the transaction to get the authorization entry
    KeyPair keyPair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());
    TransactionBuilderAccount source = rpcClient.getAccount(keyPair.getAccountId());

    SCVal[] args = createArgsFromRequest(challengeRequest);
    InvokeHostFunctionOperation operation =
        InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                sep10Config.getWebAuthContract(), WEB_AUTH_VERIFY_FN, Arrays.asList(args))
            .sourceAccount(source.getAccountId())
            .build();
    Transaction transaction =
        new TransactionBuilder(source, new Network(appConfig.getStellarNetworkPassphrase()))
            .setBaseFee(Transaction.MIN_BASE_FEE)
            .addOperation(operation)
            .setTimeout(300)
            .build();
    Log.debugF("Challenge transaction: {}", transaction.toEnvelopeXdrBase64());

    // Simulate transaction to get auth entries
    SimulateTransactionResponse simulateTransactionResponse =
        rpcClient.simulateTransaction(transaction);
    Log.debugF("Simulate transaction response: {}", simulateTransactionResponse);

    SorobanAuthorizationEntry authorizationEntry;
    try {
      authorizationEntry =
          SorobanAuthorizationEntry.fromXdrBase64(
              simulateTransactionResponse.getResults().get(0).getAuth().get(0));
    } catch (IOException e) {
      throw new SepException("Failed to decode challenge invocation", e);
    }

    // Sign the invocation and return the challenge to the client
    try {
      return ChallengeResponse.builder()
          .authorizationEntry(authorizationEntry.toXdrBase64())
          .serverSignature(signAuthorizationEntry(authorizationEntry))
          .build();
    } catch (IOException e) {
      throw new SepException("Unable to sign invocation", e);
    }
  }

  private SCVal[] createArgsFromRequest(ChallengeRequest challengeRequest) {
    Map<String, String> args = new LinkedHashMap<>();
    if (challengeRequest.getAddress() != null) args.put("address", challengeRequest.getAddress());
    if (sep10Config.getWebAuthDomain() != null)
      args.put("web_auth_domain", sep10Config.getWebAuthDomain());
    if (challengeRequest.getHomeDomain() != null)
      args.put("home_domain", challengeRequest.getHomeDomain());
    if (challengeRequest.getMemo() != null) args.put("memo", challengeRequest.getMemo());
    if (challengeRequest.getClientDomain() != null)
      args.put("client_domain", challengeRequest.getClientDomain());

    return createArguments(args);
  }

  private SCVal[] createArguments(Map<String, String> args) {
    SCMapEntry[] entries =
        args.entrySet().stream()
            .map(
                entry ->
                    new SCMapEntry.Builder()
                        .key(Scv.toString(entry.getKey()))
                        .val(Scv.toString(entry.getValue()))
                        .build())
            .toArray(SCMapEntry[]::new);
    SCMap scMap = new SCMap(entries);

    return new SCVal[] {new SCVal.Builder().discriminant(SCValType.SCV_MAP).map(scMap).build()};
  }

  /**
   * Validates the challenge signed by the client.
   *
   * @param validationRequest The validation request.
   * @return The validation response.
   */
  public ValidationResponse validateChallenge(ValidationRequest validationRequest)
      throws SepException {
    SorobanAuthorizationEntry authorizationEntry;
    try {
      authorizationEntry =
          SorobanAuthorizationEntry.fromXdrBase64(validationRequest.getAuthorizationEntry());
    } catch (IOException e) {
      throw new SepValidationException("Failed to decode challenge invocation", e);
    }

    // Verify the server signature
    String expectedSignature = signAuthorizationEntry(authorizationEntry);
    if (!expectedSignature.equals(validationRequest.getServerSignature())) {
      throw new SepValidationException("Server signature is invalid");
    }

    // Verify the client signature by simulating the invocation
    SorobanCredentials clientCredentials;
    try {
      clientCredentials = SorobanCredentials.fromXdrBase64(validationRequest.getCredentials());
    } catch (IOException e) {
      throw new SepException("Failed to decode client credentials", e);
    }

    KeyPair keyPair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());
    TransactionBuilderAccount source = rpcClient.getAccount(keyPair.getAccountId());

    SorobanAuthorizationEntry authorizationEntryClone;
    try {
      authorizationEntryClone =
          SorobanAuthorizationEntry.fromXdrBase64(validationRequest.getAuthorizationEntry());
      authorizationEntryClone.setCredentials(clientCredentials);
      Log.debugF("Client signed auth entry: {}", authorizationEntryClone.toXdrBase64());
    } catch (IOException e) {
      throw new SepException("Failed to decode challenge authorization entry", e);
    }

    SCVal[] parameters =
        authorizationEntry.getRootInvocation().getFunction().getContractFn().getArgs();
    InvokeHostFunctionOperation operation =
        InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                sep10Config.getWebAuthContract(), WEB_AUTH_VERIFY_FN, Arrays.asList(parameters))
            .sourceAccount(source.getAccountId())
            .auth(Collections.singletonList(authorizationEntryClone))
            .build();

    Network network = new Network(appConfig.getStellarNetworkPassphrase());
    Transaction transaction =
        new TransactionBuilder(source, network)
            .setBaseFee(Transaction.MIN_BASE_FEE)
            .addOperation(operation)
            .setTimeout(300)
            .build();

    SimulateTransactionResponse response = this.rpcClient.simulateTransaction(transaction);
    if (response.getError() != null) {
      throw new SepValidationException("Error validating credentials: " + response.getError());
    }
    Log.debugF("Credentials successfully validated: {}", response);

    return ValidationResponse.builder().token(generateJwt(authorizationEntry)).build();
  }

  private String generateJwt(SorobanAuthorizationEntry authorizationEntry) throws SepException {
    SorobanAuthorizedInvocation invocation = authorizationEntry.getRootInvocation();

    long issuedAt = Instant.now().getEpochSecond();
    String address = getFromContractArgs(invocation, "address").orElse(null);
    String memo = getFromContractArgs(invocation, "memo").orElse(null);
    String homeDomain = getFromContractArgs(invocation, "home_domain").orElse(null);
    String clientDomain = getFromContractArgs(invocation, "client_domain").orElse(null);

    String authUrl = "https://" + sep10Config.getWebAuthDomainC() + "/auth";
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
            homeDomain,
            clientDomain);

    return jwtService.encode(jwt);
  }

  private Optional<String> getFromContractArgs(SorobanAuthorizedInvocation invocation, String key) {
    SCString scKey = Scv.toString(key).getStr();
    SCMap args = invocation.getFunction().getContractFn().getArgs()[0].getMap();
    for (SCMapEntry entry : args.getSCMap()) {
      if (entry.getKey().getStr().equals(scKey)) {
        return Optional.of(entry.getVal().getStr().getSCString().toString());
      }
    }
    return Optional.empty();
  }

  /**
   * Signs the authorization entry with the server secret and returns the hex-encoded signature.
   *
   * @param entry the authorization entry to sign
   * @return the server signature
   */
  private String signAuthorizationEntry(SorobanAuthorizationEntry entry) {
    KeyPair keypair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());

    try {
      byte[] hash = Util.hash(entry.toXdrByteArray());
      byte[] signature = keypair.sign(hash);

      return Util.bytesToHex(signature);
    } catch (IOException e) {
      throw new RuntimeException("Unable to decode authorization entry", e);
    }
  }
}
