package org.stellar.anchor.platform.integrationtest

import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest
import org.stellar.anchor.client.Sep10CClient
import org.stellar.anchor.client.Sep24Client
import org.stellar.anchor.platform.AbstractIntegrationTests
import org.stellar.anchor.platform.TestConfig
import org.stellar.sdk.SorobanServer

class Sep24CTests : AbstractIntegrationTests(TestConfig()) {
  private var sep10CClient: Sep10CClient =
    Sep10CClient(
      toml.getString("WEB_AUTH_ENDPOINT_C"),
      toml.getString("SIGNING_KEY"),
      SorobanServer("https://soroban-testnet.stellar.org"),
    )
  private var sep24Client: Sep24Client
  private val webAuthDomain = toml.getString("WEB_AUTH_ENDPOINT_C")
  private val clientWalletContractAddress =
    "CDYOQJLKZWHZ2CVN43EVEQNDLEN544IGCO5A52UG4YS6KDN5QQ2LUWKY"

  init {
    val challenge =
      sep10CClient.getChallenge(
        ChallengeRequest.builder()
          .address(clientWalletContractAddress)
          .homeDomain(webAuthDomain)
          .build()
      )
    val validationRequest = sep10CClient.sign(challenge)
    val token = sep10CClient.validate(validationRequest).token

    sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), token)
  }

  @Test
  fun testWithdraw() = runBlocking {
    val withdrawRequest =
      mapOf(
        "amount" to "1",
        "asset_code" to "USDC",
        "asset_issuer" to "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
        "account" to clientWalletContractAddress,
        "lang" to "en",
      )

    val response = sep24Client.withdraw(withdrawRequest)
    val transaction = sep24Client.getTransaction(response.id, "USDC").transaction

    assertEquals(response.id, transaction.id)
    assertNotNull(transaction.moreInfoUrl)
    assertEquals("incomplete", transaction.status)
    assertEquals(clientWalletContractAddress, transaction.from)
  }
}
