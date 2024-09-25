package org.stellar.anchor.platform.integrationtest

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest
import org.stellar.anchor.client.Sep10CClient
import org.stellar.anchor.platform.AbstractIntegrationTests
import org.stellar.anchor.platform.TestConfig
import org.stellar.sdk.SorobanServer
import org.stellar.walletsdk.asset.IssuedAssetId
import org.stellar.walletsdk.auth.AuthToken

class Sep24CTests : AbstractIntegrationTests(TestConfig()) {
  private val sep10CClient =
    Sep10CClient(
      toml.getString("WEB_AUTH_ENDPOINT_C"),
      toml.getString("SIGNING_KEY"),
      SorobanServer("https://soroban-testnet.stellar.org"),
    )
  private var webAuthDomain = toml.getString("WEB_AUTH_ENDPOINT_C")
  private var clientWalletContractAddress =
    "CDYOQJLKZWHZ2CVN43EVEQNDLEN544IGCO5A52UG4YS6KDN5QQ2LUWKY"

  @Test
  fun testWithdraw() = runBlocking {
    val challenge =
      sep10CClient.getChallenge(
        ChallengeRequest.builder()
          .address(clientWalletContractAddress)
          .homeDomain(webAuthDomain)
          .build()
      )
    val validationRequest = sep10CClient.sign(challenge)
    val token = sep10CClient.validate(validationRequest).token

    val withdrawRequest =
      mapOf(
        "amount" to "1",
        "asset_code" to "USDC",
        "asset_issuer" to "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
        "account" to clientWalletContractAddress,
        "lang" to "en",
      )

    val response =
      anchor
        .sep24()
        .withdraw(
          IssuedAssetId("USDC", "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"),
          AuthToken.from(token),
          withdrawRequest,
        )
    println("Withdraw response: $response")
  }
}
