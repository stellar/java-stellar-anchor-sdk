package org.stellar.anchor.sep10;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest;
import org.stellar.anchor.api.sep.sep10c.ChallengeResponse;
import org.stellar.anchor.api.sep.sep10c.ValidationRequest;
import org.stellar.anchor.api.sep.sep10c.ValidationResponse;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.rpc.RpcClient;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;
import org.stellar.sdk.*;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.requests.sorobanrpc.SorobanRpcErrorResponse;
import org.stellar.sdk.responses.sorobanrpc.SimulateTransactionResponse;
import org.stellar.sdk.scval.Scv;
import org.stellar.sdk.xdr.*;

@AllArgsConstructor
public class Sep10CService {
  private final AppConfig appConfig;
  private final SecretConfig secretConfig;
  private final Sep10Config sep10Config;
  private final RpcClient rpcClient;

  private static final String WEB_AUTH_CONTRACT_ADDRESS =
      "CC3HX7UUTL43JXMVOM2SVVCWNBWA4ZTIBJ5WWJ5SGI7EILKQ3OKEINSK";
  private static final String WEB_AUTH_VERIFY_FN = "web_auth_verify";

  /**
   * Creates a challenge for the client to sign.
   *
   * @param challengeRequest The challenge request.
   * @return The challenge response.
   */
  public ChallengeResponse createChallenge(ChallengeRequest challengeRequest) {
    Log.debugF(
        "Creating challenge for request {}", GsonUtils.getInstance().toJson(challengeRequest));

    // Simulate the transaction to get the authorization entry
    KeyPair keyPair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());
    TransactionBuilderAccount source;
    try {
      source = rpcClient.getServer().getAccount(keyPair.getAccountId());
    } catch (IOException | AccountNotFoundException | SorobanRpcErrorResponse e) {
      throw new RuntimeException("Unable to fetch account", e);
    }

    SCVal[] args = createArgsFromRequest(challengeRequest);
    InvokeHostFunctionOperation operation =
        InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                WEB_AUTH_CONTRACT_ADDRESS, WEB_AUTH_VERIFY_FN, Arrays.asList(args))
            .sourceAccount(source.getAccountId())
            .build();
    Transaction transaction =
        new TransactionBuilder(source, new Network(appConfig.getStellarNetworkPassphrase()))
            .setBaseFee(Transaction.MIN_BASE_FEE)
            .addOperation(operation)
            .setTimeout(300)
            .build();
    Log.debugF("Transaction: {}", transaction.toEnvelopeXdrBase64());

    SorobanAuthorizationEntry authorizationEntry;
    try {
      SimulateTransactionResponse simulateTransactionResponse =
          rpcClient.getServer().simulateTransaction(transaction);
      Log.debugF("Simulate transaction response: {}", simulateTransactionResponse);
      authorizationEntry =
          SorobanAuthorizationEntry.fromXdrBase64(
              simulateTransactionResponse.getResults().get(0).getAuth().get(0));
    } catch (IOException | SorobanRpcErrorResponse e) {
      throw new RuntimeException("Error simulating transaction", e);
    }

    // Sign the invocation and return the challenge to the client
    try {
      return ChallengeResponse.builder()
          .authorizationEntry(authorizationEntry.toXdrBase64())
          .serverSignature(signAuthorizationEntry(authorizationEntry))
          .build();
    } catch (IOException e) {
      throw new RuntimeException("Unable to sign invocation", e);
    }
  }

  private SCVal[] createArgsFromRequest(ChallengeRequest challengeRequest) {
    Map<String, String> args = new LinkedHashMap<>();
    if (challengeRequest.getAccount() != null) args.put("account", challengeRequest.getAccount());
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
  public ValidationResponse validateChallenge(ValidationRequest validationRequest) {
    SorobanAuthorizationEntry authorizationEntry;
    try {
      authorizationEntry =
          SorobanAuthorizationEntry.fromXdrBase64(validationRequest.getAuthorizationEntry());
    } catch (IOException e) {
      throw new RuntimeException("Failed to decode challenge invocation", e);
    }

    // Verify the server signature
    try {
      String expectedCredentials = signAuthorizationEntry(authorizationEntry);
      if (!expectedCredentials.equals(validationRequest.getServerSignature())) {
        throw new RuntimeException("Server credentials do not match expected credentials");
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to sign invocation ", e);
    }

    // Verify the client signature by simulating the invocation
    SorobanCredentials clientCredentials;
    try {
      clientCredentials = SorobanCredentials.fromXdrBase64(validationRequest.getCredentials());
    } catch (IOException e) {
      throw new RuntimeException("Failed to decode client credentials", e);
    }

    KeyPair keyPair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());
    TransactionBuilderAccount source;
    try {
      source = rpcClient.getServer().getAccount(keyPair.getAccountId());
    } catch (IOException | AccountNotFoundException | SorobanRpcErrorResponse e) {
      throw new RuntimeException("Unable to fetch account", e);
    }

    SCVal[] parameters =
        authorizationEntry.getRootInvocation().getFunction().getContractFn().getArgs();
    InvokeHostFunctionOperation operation =
        InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                WEB_AUTH_CONTRACT_ADDRESS, WEB_AUTH_CONTRACT_ADDRESS, Arrays.asList(parameters))
            .sourceAccount(source.getAccountId())
            .auth(
                Collections.singletonList(
                    new SorobanAuthorizationEntry.Builder()
                        .credentials(clientCredentials)
                        .rootInvocation(authorizationEntry.getRootInvocation())
                        .build()))
            .build();

    Network network = new Network(appConfig.getStellarNetworkPassphrase());
    Transaction transaction =
        new TransactionBuilder(source, network)
            .setBaseFee(Transaction.MIN_BASE_FEE)
            .addOperation(operation)
            .setTimeout(300)
            .build();

    try {
      this.rpcClient.getServer().simulateTransaction(transaction);
    } catch (IOException | SorobanRpcErrorResponse e) {
      throw new RuntimeException("Error simulating transaction", e);
    }

    // TODO: return the JWT
    return ValidationResponse.builder().build();
  }

  /**
   * Signs the invocation with the server secret and returns the signature.
   *
   * @param invocation The invocation to sign.
   * @return the server signature
   * @throws IOException If an error occurs.
   */
  private String signAuthorizationEntry(SorobanAuthorizationEntry invocation) throws IOException {
    KeyPair keypair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());

    byte[] hash = Util.hash(invocation.toXdrByteArray());
    byte[] signature = keypair.sign(hash);

    // return the signature in hex
    return Util.bytesToHex(signature);
  }
}
