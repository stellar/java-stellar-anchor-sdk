package org.stellar.anchor.platform

import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest
import org.stellar.anchor.client.Sep10CClient
import org.stellar.anchor.util.Sep1Helper.TomlContent
import org.stellar.anchor.util.Sep1Helper.parse
import org.stellar.sdk.KeyPair
import org.stellar.sdk.SorobanServer
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.anchor.auth
import org.stellar.walletsdk.auth.AuthToken
import org.stellar.walletsdk.horizon.SigningKeyPair

abstract class AbstractIntegrationTests(val config: TestConfig) {
  var toml: TomlContent =
    parse(resourceAsString("${config.env["anchor.domain"]}/.well-known/stellar.toml"))
  var wallet =
    Wallet(
      StellarConfiguration.Testnet,
      ApplicationConfiguration { defaultRequest { url { protocol = URLProtocol.HTTP } } },
    )
  var walletKeyPair = SigningKeyPair.fromSecret(CLIENT_WALLET_SECRET)
  var anchor = wallet.anchor(config.env["anchor.domain"]!!)
  var token: AuthToken

  var sep10CClient =
    Sep10CClient(
      toml.getString("WEB_AUTH_ENDPOINT_C"),
      toml.getString("SIGNING_KEY"),
      SorobanServer("https://soroban-testnet.stellar.org"),
    )
  val smartWalletKeyPair = KeyPair.fromSecretSeed(SMART_WALLET_SIGNER_SECRET)
  var smartWalletSep10Jwt: String

  private val submissionLock = Mutex()

  suspend fun transactionWithRetry(
    maxAttempts: Int = 5,
    delay: Int = 5,
    transactionLogic: suspend () -> Unit,
  ) =
    flow<Unit> { submissionLock.withLock { transactionLogic() } }
      .retryWhen { _, attempt ->
        if (attempt < maxAttempts) {
          delay((delay + (1..5).random()).seconds)
          return@retryWhen true
        } else {
          return@retryWhen false
        }
      }
      .collect {}

  init {
    runBlocking {
      token = anchor.auth().authenticate(walletKeyPair)

      // Setup SEP-10c
      val challenge =
        sep10CClient.getChallenge(
          ChallengeRequest.builder()
            .address(SMART_WALLET_ADDRESS)
            .homeDomain(toml.getString("WEB_AUTH_ENDPOINT_C"))
            .build()
        )
      val validationRequest = sep10CClient.sign(challenge)
      smartWalletSep10Jwt = sep10CClient.validate(validationRequest).token
    }
  }
}
