package org.stellar.anchor.platform.subtest

import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.sep.sep10.ValidationRequest
import org.stellar.anchor.platform.*

class Sep10Tests : SepTests(TestConfig(testProfileName = "default")) {
  lateinit var sep10Client: Sep10Client
  lateinit var sep10ClientMultiSig: Sep10Client

  init {
    if (!::sep10Client.isInitialized) {
      sep10Client =
        Sep10Client(
          toml.getString("WEB_AUTH_ENDPOINT"),
          toml.getString("SIGNING_KEY"),
          CLIENT_WALLET_ACCOUNT,
          CLIENT_WALLET_SECRET
        )
    }
    if (!::sep10ClientMultiSig.isInitialized) {
      sep10ClientMultiSig =
        Sep10Client(
          toml.getString("WEB_AUTH_ENDPOINT"),
          toml.getString("SIGNING_KEY"),
          CLIENT_WALLET_ACCOUNT,
          arrayOf(
            CLIENT_WALLET_SECRET,
            CLIENT_WALLET_EXTRA_SIGNER_1_SECRET,
            CLIENT_WALLET_EXTRA_SIGNER_2_SECRET
          )
        )
    }
  }

  @Test
  fun testAuth() {
    sep10Client.auth()
  }

  @Test
  fun testMultiSig() {
    sep10ClientMultiSig.auth()
  }

  @Test
  fun testUnsignedChallenge() {
    val challenge = sep10Client.challenge()

    assertFailsWith(
      exceptionClass = SepNotAuthorizedException::class,
      block = { sep10Client.validate(ValidationRequest.of(challenge.transaction)) }
    )
  }
}
