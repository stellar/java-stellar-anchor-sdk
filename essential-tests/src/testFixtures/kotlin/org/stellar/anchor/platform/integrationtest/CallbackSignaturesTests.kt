package org.stellar.anchor.platform.integrationtest

import java.util.Base64
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.stellar.anchor.platform.assertCallbackSignatures
import org.stellar.reference.wallet.RawCallback
import org.stellar.sdk.KeyPair

/**
 * The shared callback verification helper, proven to pass a correct callback and to fail each way a
 * callback can be wrong. Without these, a helper that stopped checking something would pass every
 * e2e flow that uses it.
 */
class CallbackSignaturesTests {
  private val host = "wallet-server:8092"
  private val body =
    """{"transaction":{"id":"txn-1","status":"completed"},"padding":"xxxxxxxxxxxx"}"""
  private val anchorKey = KeyPair.random()
  private val now = System.currentTimeMillis() / 1000

  private fun signed(
    t: Long = now,
    signHost: String = host,
    signBody: String = body,
    sentBody: String = signBody,
    key: KeyPair = anchorKey,
  ): RawCallback {
    val signature = key.sign("$t.$signHost.$signBody".toByteArray())
    return RawCallback("t=$t, s=${Base64.getEncoder().encodeToString(signature)}", host, sentBody)
  }

  private fun check(
    callbacks: List<RawCallback>,
    signingKey: String = anchorKey.accountId,
    expectedHost: String? = host,
    expectedCount: Int = callbacks.size,
    notBefore: Long = now - 5,
  ) = assertCallbackSignatures(callbacks, signingKey, expectedHost, expectedCount, notBefore)

  // SEP1CS-04
  @Test
  fun `test a correct callback passes`() {
    assertDoesNotThrow { check(listOf(signed())) }
    assertDoesNotThrow { check(listOf(signed(), signed(t = now - 1))) }
  }

  // SEP1CS-03
  @Test
  fun `test a callback signed by another key fails`() {
    assertThrows(AssertionError::class.java) { check(listOf(signed(key = KeyPair.random()))) }
  }

  // SEP1CS-03
  @Test
  fun `test a callback verified against another public key fails`() {
    assertThrows(AssertionError::class.java) {
      check(listOf(signed()), signingKey = KeyPair.random().accountId)
    }
  }

  // SEP1CS-03
  @Test
  fun `test a callback received on another host than the signed one fails`() {
    assertThrows(AssertionError::class.java) {
      check(listOf(signed()), expectedHost = "other:8092")
    }
    assertThrows(AssertionError::class.java) { check(listOf(signed(signHost = "other:8092"))) }
  }

  // SEP1CS-03
  @Test
  fun `test a body changed after signing fails`() {
    assertThrows(AssertionError::class.java) {
      check(listOf(signed(sentBody = body.replace("completed", "refunded"))))
    }
  }

  // SEP1CS-03
  @Test
  fun `test a timestamp outside the window fails`() {
    assertThrows(AssertionError::class.java) { check(listOf(signed(t = now - 600))) }
    assertThrows(AssertionError::class.java) { check(listOf(signed(t = now + 3600))) }
  }

  // SEP1CS-03
  @Test
  fun `test a malformed Signature header fails`() {
    val valid = signed()
    listOf(null, "", "t=$now", "t=$now s=abc", "s=abc, t=$now", "t=abc, s=abc").forEach { header ->
      assertThrows(
        AssertionError::class.java,
        { check(listOf(valid.copy(signature = header))) },
        "header: $header"
      )
    }
  }

  // SEP1CS-03
  @Test
  fun `test a different number of callbacks than expected fails`() {
    assertThrows(AssertionError::class.java) { check(listOf(signed()), expectedCount = 2) }
    assertThrows(AssertionError::class.java) { check(emptyList(), expectedCount = 1) }
  }

  // SEP1CS-03: a real signature in a wrong envelope must fail at the header, not at the signature
  @Test
  fun `test a valid signature in a wrong header envelope fails`() {
    val valid = signed()
    val s = valid.signature!!.substringAfter("s=")
    listOf(
        "t=$now,s=$s",
        "s=$s, t=$now",
        "t=$now, s=$s, x=1",
        " t=$now, s=$s",
        "t=$now, s=$s ",
        "t=-1, s=$s",
      )
      .forEach { header ->
        assertThrows(
          AssertionError::class.java,
          { check(listOf(valid.copy(signature = header))) },
          "header: $header"
        )
      }
  }

  // SEP1CS-03, SEP1CS-04: the window bounds are inclusive at the start and exclusive before it
  @Test
  fun `test the lower bound of the timestamp window`() {
    assertDoesNotThrow { check(listOf(signed(t = now - 5)), notBefore = now - 5) }
    assertThrows(AssertionError::class.java) {
      check(listOf(signed(t = now - 6)), notBefore = now - 5)
    }
  }

  // SEP1CS-03: more callbacks than expected
  @Test
  fun `test more callbacks than expected fails`() {
    assertThrows(AssertionError::class.java) {
      check(listOf(signed(), signed()), expectedCount = 1)
    }
  }
}
