package org.stellar.anchor.sep10;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
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
    long nonce = generateNonce();

    // TODO: we need to make sure the transaction fails if submitted.
    SorobanAuthorizedInvocation invocation =
        new SorobanAuthorizedInvocation.Builder()
            .function(
                new SorobanAuthorizedFunction.Builder()
                    .discriminant(
                        SorobanAuthorizedFunctionType.SOROBAN_AUTHORIZED_FUNCTION_TYPE_CONTRACT_FN)
                    .contractFn(
                        new InvokeContractArgs.Builder()
                            .contractAddress(new Address(WEB_AUTH_CONTRACT_ADDRESS).toSCAddress())
                            .functionName(Scv.toSymbol(WEB_AUTH_VERIFY_FN).getSym())
                            .args(createArgsFromRequest(challengeRequest))
                            .build())
                    .build())
            .subInvocations(new SorobanAuthorizedInvocation[0])
            .build();

    // Sign the invocation and return the challenge to the client
    try {
      return ChallengeResponse.builder()
          .authorizedInvocation(invocation.toXdrBase64())
          .serverSignature(signInvocation(invocation))
          .build();
    } catch (IOException e) {
      throw new RuntimeException("Unable to sign invocation", e);
    }
  }

  private long generateNonce() {
    byte[] nonce = new byte[32];
    SecureRandom secureRandom = new SecureRandom();
    secureRandom.nextBytes(nonce);
    return new BigInteger(nonce).longValue();
  }

  private SCVal[] createArgsFromRequest(ChallengeRequest challengeRequest) {
    Map<String, String> args = new LinkedHashMap<>();
    if (sep10Config.getWebAuthDomain() != null)
      args.put("web_auth_domain", sep10Config.getWebAuthDomain());
    if (challengeRequest.getHomeDomain() != null)
      args.put("home_domain", challengeRequest.getHomeDomain());
    if (challengeRequest.getAccount() != null) args.put("account", challengeRequest.getAccount());
    if (challengeRequest.getMemo() != null) args.put("memo", challengeRequest.getMemo());
    if (challengeRequest.getClientDomain() != null)
      args.put("client_domain", challengeRequest.getClientDomain());
    args.put("nonce", String.valueOf(generateNonce()));

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
    SorobanAuthorizedInvocation invocation;
    try {
      invocation = SorobanAuthorizedInvocation.fromXdrBase64(validationRequest.getInvocation());
    } catch (IOException e) {
      throw new RuntimeException("Failed to decode challenge invocation", e);
    }

    // Verify the server signature
    try {
      String expectedCredentials = signInvocation(invocation);
      if (!expectedCredentials.equals(validationRequest.getServerCredentials())) {
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

    SCVal[] parameters = invocation.getFunction().getContractFn().getArgs();
    InvokeHostFunctionOperation operation =
        InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                WEB_AUTH_CONTRACT_ADDRESS, WEB_AUTH_CONTRACT_ADDRESS, Arrays.asList(parameters))
            .sourceAccount(source.getAccountId())
            .auth(
                Collections.singletonList(
                    new SorobanAuthorizationEntry.Builder()
                        .credentials(clientCredentials)
                        .rootInvocation(invocation)
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
  private String signInvocation(SorobanAuthorizedInvocation invocation) throws IOException {
    KeyPair keypair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());

    byte[] hash = Util.hash(invocation.toXdrByteArray());
    byte[] signature = keypair.sign(hash);

    // return the signature in hex
    return Util.bytesToHex(signature);
  }
}
