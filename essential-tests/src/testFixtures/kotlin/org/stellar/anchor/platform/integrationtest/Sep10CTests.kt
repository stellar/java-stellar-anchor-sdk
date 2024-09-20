package org.stellar.anchor.platform.integrationtest

import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest
import org.stellar.anchor.client.Sep10CClient
import org.stellar.anchor.platform.AbstractIntegrationTests
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.Log
import org.stellar.sdk.SorobanServer

class Sep10CTests : AbstractIntegrationTests(TestConfig()) {
  private var sep10CClient: Sep10CClient =
    Sep10CClient(
      "http://localhost:8080/sep10c/auth",
      toml.getString("SIGNING_KEY"),
      SorobanServer("https://soroban-testnet.stellar.org")
    )
  private var webAuthDomain = toml.getString("WEB_AUTH_DOMAIN")
  private var clientWalletContractAddress =
    "CC3HX7UUTL43JXMVOM2SVVCWNBWA4ZTIBJ5WWJ5SGI7EILKQ3OKEINSK"

  @Test
  fun testChallengeSigning() {
    val challenge =
      sep10CClient.getChallenge(
        ChallengeRequest.builder()
          .account(clientWalletContractAddress)
          .homeDomain(webAuthDomain)
          .build()
      )
    Log.info("authorizationEntry: ${challenge.authorizationEntry}")
    Log.info("signature: ${challenge.serverSignature}")

    sep10CClient.sign(challenge)
  }
}
