package org.stellar.anchor.platform

import kotlin.test.assertFailsWith
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.sep.sep10.ValidationRequest
import org.stellar.anchor.util.Sep1Helper

var CLIENT_WALLET_EXTRA_SIGNER_1 = "GC6X2ANA2OS3O2ESHUV6X44NH6J46EP2EO2JB7563Y7DYOIXFKHMHJ5O"
var CLIENT_WALLET_EXTRA_SIGNER_1_SECRET = "SDUDRKCL5AX7RDZ7S6JAPBNKLV6LRZXJSHN5OJDP32TIJB42ODPQODHY"
var CLIENT_WALLET_EXTRA_SIGNER_2 = "GATEYCIMJZ2F6Y437QSYH4XFQ6HLD5YP4MBJZFFPZVEQDJOY4QTCB7BB"
var CLIENT_WALLET_EXTRA_SIGNER_2_SECRET = "SC52GRNSIODLPNGTXUCZ5NHBII4PYUKUVQCCWWIK6OB6P4AW4M37DXZK"

lateinit var sep10Client: Sep10Client
lateinit var sep10ClientMultiSig: Sep10Client

fun sep10TestAll(toml: Sep1Helper.TomlContent): String {
  println("Performing SEP10 tests...")
  sep10Client =
    Sep10Client(
      toml.getString("WEB_AUTH_ENDPOINT"),
      toml.getString("SIGNING_KEY"),
      CLIENT_WALLET_ACCOUNT,
      CLIENT_WALLET_SECRET
    )

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

  val jwt = testOk()
  testUnsignedChallenge()
  testMultiSig()

  return jwt
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
