package org.stellar.anchor.sep10;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import lombok.AllArgsConstructor;
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest;
import org.stellar.anchor.api.sep.sep10c.ChallengeResponse;
import org.stellar.anchor.api.sep.sep10c.ValidationRequest;
import org.stellar.anchor.api.sep.sep10c.ValidationResponse;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.rpc.RpcClient;
import org.stellar.sdk.Address;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.Util;
import org.stellar.sdk.scval.Scv;
import org.stellar.sdk.xdr.*;

@AllArgsConstructor
public class Sep10CService {
  private final AppConfig appConfig;
  private final SecretConfig secretConfig;
  private final Sep10Config sep10Config;
  private final RpcClient rpcClient;

  /**
   * Creates a challenge for the client to sign.
   *
   * @param challengeRequest The challenge request.
   * @return The challenge response.
   */
  public ChallengeResponse createChallenge(ChallengeRequest challengeRequest) {
    byte[] nonce = new byte[32];
    SecureRandom secureRandom = new SecureRandom();
    secureRandom.nextBytes(nonce);
    int intNonce = new java.math.BigInteger(nonce).intValue();

    SCVal[] arguments =
        new SCVal[] {
          /* web_auth_domain= */ Scv.toString(sep10Config.getWebAuthDomain()),
          /* home_domain= */ Scv.toString(challengeRequest.getHomeDomain()),
          /* account_id= */ Scv.toString(challengeRequest.getAccount()),
          /* nonce= */ Scv.toUint32(intNonce),
          // TODO: memo?
        };

    // TODO: we need to make sure the transaction fails if submitted
    SorobanAuthorizedInvocation invocation =
        new SorobanAuthorizedInvocation.Builder()
            .function(
                new SorobanAuthorizedFunction.Builder()
                    .discriminant(
                        SorobanAuthorizedFunctionType.SOROBAN_AUTHORIZED_FUNCTION_TYPE_CONTRACT_FN)
                    .contractFn(
                        new InvokeContractArgs.Builder()
                            .contractAddress(
                                new Address(challengeRequest.getAccount()).toSCAddress())
                            // Assume the smart wallet interface defines this method
                            .functionName(Scv.toSymbol("web_auth_verify").getSym())
                            .args(arguments)
                            .build())
                    .build())
            .subInvocations(new SorobanAuthorizedInvocation[0])
            .build();

    // Sign the invocation and return the challenge to the client
    // TODO: do we need to simulate it?
    SorobanCredentials credentials;
    try {
      credentials = signInvocation(invocation, intNonce);

      return ChallengeResponse.builder()
          .authorizedInvocation(invocation.toXdrBase64())
          .serverCredentials(credentials.toXdrBase64())
          .build();
    } catch (IOException e) {
      throw new RuntimeException("Unable to sign invocation ", e);
    }
  }

  public ValidationResponse validateChallenge(ValidationRequest validationRequest) {
    return ValidationResponse.builder().build();
  }

  /**
   * Signs the invocation with the server secret and returns the credentials.
   *
   * @param invocation The invocation to sign.
   * @param nonce The nonce to use.
   * @return The signed credentials.
   * @throws IOException If an error occurs.
   */
  private SorobanCredentials signInvocation(SorobanAuthorizedInvocation invocation, int nonce)
      throws IOException {
    Network network = new Network(appConfig.getStellarNetworkPassphrase());
    // TODO: fetch from RPC
    long validUntilLedgerSequence = 0;
    KeyPair keypair = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());

    HashIDPreimage preimage =
        new HashIDPreimage.Builder()
            .discriminant(EnvelopeType.ENVELOPE_TYPE_SOROBAN_AUTHORIZATION)
            .sorobanAuthorization(
                new HashIDPreimage.HashIDPreimageSorobanAuthorization.Builder()
                    .networkID(new Hash(network.getNetworkId()))
                    // TODO: should we be using same nonce everywhere?
                    .nonce(new Int64((long) nonce))
                    .invocation(invocation)
                    .signatureExpirationLedger(
                        new Uint32(new XdrUnsignedInteger(validUntilLedgerSequence)))
                    .build())
            .build();

    byte[] preimageBytes = preimage.toXdrByteArray();
    byte[] hash = Util.hash(preimageBytes);
    byte[] signature = keypair.sign(hash);

    return new SorobanCredentials.Builder()
        .discriminant(SorobanCredentialsType.SOROBAN_CREDENTIALS_ADDRESS)
        .address(
            new SorobanAddressCredentials.Builder()
                .address(new Address(keypair.getAccountId()).toSCAddress())
                .nonce(new Int64((long) nonce))
                .signatureExpirationLedger(
                    new Uint32(new XdrUnsignedInteger(validUntilLedgerSequence)))
                .signature(
                    Scv.toVec(
                        Collections.singleton(
                            Scv.toMap(
                                new LinkedHashMap<>() {
                                  {
                                    put(
                                        Scv.toSymbol("public_key"),
                                        Scv.toBytes(keypair.getPublicKey()));
                                    put(Scv.toSymbol("signature"), Scv.toBytes(signature));
                                  }
                                }))))
                .build())
        .build();
  }
}
