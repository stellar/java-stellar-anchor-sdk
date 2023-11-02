package org.stellar.anchor.platform.test

import kotlin.test.assertFailsWith
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.sep.sep10.ValidationRequest
import org.stellar.anchor.platform.*
import org.stellar.anchor.util.Sep1Helper.TomlContent

class Sep10Tests(val toml: TomlContent) {
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

  fun testMultiSig() {
    sep10ClientMultiSig.auth()
  }

  fun testOk(): String {
    return sep10Client.auth()
  }

  fun testUnsignedChallenge() {
    val challenge = sep10Client.challenge()

    assertFailsWith(
      exceptionClass = SepNotAuthorizedException::class,
      block = { sep10Client.validate(ValidationRequest.of(challenge.transaction)) }
    )
  }

  fun testAll() {
    println("Performing SEP10 tests...")

    testOk()
    testUnsignedChallenge()
    testMultiSig()
  }
}
