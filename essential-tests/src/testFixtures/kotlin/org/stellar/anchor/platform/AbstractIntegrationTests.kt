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
    const val TEST_MEMO = "22bf7341574e4b1082516a2e84a8"
    const val TEST_PAYMENT_ID = "939223448317953"
    const val TEST_PAYMENT_AMOUNT = "1.0000000"
    const val TEST_STELLAR_TRANSACTION_HASH =
      "1238508fd0e8f5a56507a51044aa741ea67b65f2d8044e281a212e580b8dc9d8"
    const val TEST_STELLAR_TRANSACTION_SOURCE_ACCOUNT =
      "GABCKCYPAGDDQMSCTMSBO7C2L34NU3XXCW7LR4VVSWCCXMAJY3B4YCZP"
    const val TEST_STELLAR_TRANSACTION_DEST_ACCOUNT =
      "GBDYDBJKQBJK4GY4V7FAONSFF2IBJSKNTBYJ65F5KCGBY2BIGPGGLJOH"
    const val TEST_ASSET_USDC = "USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
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
