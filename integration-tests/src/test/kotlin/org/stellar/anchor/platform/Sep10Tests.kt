package org.stellar.anchor.platform

import kotlin.test.assertFailsWith
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.sep.sep10.ValidationRequest
import org.stellar.anchor.util.Sep1Helper

lateinit var sep10Client: Sep10Client

fun sep10TestAll(toml: Sep1Helper.TomlContent): String {
  println("Performing SEP10 tests...")
  sep10Client =
    Sep10Client(
      toml.getString("WEB_AUTH_ENDPOINT"),
      toml.getString("SIGNING_KEY"),
      CLIENT_WALLET_ACCOUNT,
      CLIENT_WALLET_SECRET
    )

  val jwt = testOk()
  testUnsignedChallenge()

  return jwt
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
