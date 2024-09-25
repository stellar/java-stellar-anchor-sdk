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
import org.stellar.anchor.util.Sep1Helper.TomlContent
import org.stellar.anchor.util.Sep1Helper.parse
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.anchor.auth
import org.stellar.walletsdk.auth.AuthToken
import org.stellar.walletsdk.horizon.SigningKeyPair

abstract class AbstractIntegrationTests(val config: TestConfig) {
  companion object {
    const val TEST_PAYMENT_ID = "574872782651393"
    const val TEST_PAYMENT_AMOUNT = "10.0000000"
    const val TEST_STELLAR_TRANSACTION_HASH =
      "47dd0547e48513b967ba1c3cdf83cb90d450800a182b10d54e7858b32a10207f"
    const val TEST_STELLAR_TRANSACTION_SOURCE_ACCOUNT =
      "GAYF33NNNMI2Z6VNRFXQ64D4E4SF77PM46NW3ZUZEEU5X7FCHAZCMHKU"
    const val TEST_STELLAR_TRANSACTION_DEST_ACCOUNT =
      "GABCKCYPAGDDQMSCTMSBO7C2L34NU3XXCW7LR4VVSWCCXMAJY3B4YCZP"
  }

  var toml: TomlContent =
    parse(resourceAsString("${config.env["anchor.domain"]}/.well-known/stellar.toml"))
  var wallet =
    Wallet(
      StellarConfiguration.Testnet,
      ApplicationConfiguration { defaultRequest { url { protocol = URLProtocol.HTTP } } }
    )
  var walletKeyPair = SigningKeyPair.fromSecret(CLIENT_WALLET_SECRET)
  var anchor = wallet.anchor(config.env["anchor.domain"]!!)
  var token: AuthToken
  private val submissionLock = Mutex()

  suspend fun transactionWithRetry(
    maxAttempts: Int = 5,
    delay: Int = 5,
    transactionLogic: suspend () -> Unit
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
    runBlocking { token = anchor.auth().authenticate(walletKeyPair) }
  }
}
