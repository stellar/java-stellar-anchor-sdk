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
    "CA24E6YPM2FOXVVE566TD775RCRZK4GPR67QC7DBW7O722STUHTXGW3Y"

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

    val validationRequest = sep10CClient.sign(challenge)
    Log.info("credentials: ${validationRequest.credentials}")

    val validationResponse = sep10CClient.validate(validationRequest)
    Log.info("token: $validationResponse")
  }
}
