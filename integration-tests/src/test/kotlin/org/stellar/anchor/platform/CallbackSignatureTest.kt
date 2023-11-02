package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.stellar.anchor.platform.event.ClientStatusCallbackHandler
import org.stellar.reference.wallet.CallbackService.Companion.verifySignature
import org.stellar.sdk.KeyPair
import org.stellar.sdk.KeyPair.*

val signerSecret: KeyPair =
  fromSecretSeed("SAX3AH622R2XT6DXWWSRIDCMMUCCMATBZ5U6XKJWDO7M2EJUBFC3AW5X")
val signerPublic: KeyPair =
  fromAccountId("GCHLHDBOKG2JWMJQBTLSL5XG6NO7ESXI2TAQKZXCXWXB5WI2X6W233PR")

class CallbackSignatureTest {
  @Test
  fun `test the SEP24 callback signature creation and verification`() {
    // create the request with the secret-key signer
    val request =
      ClientStatusCallbackHandler.buildHttpRequest(
        signerSecret,
        "test_payload",
        "http://localhost:8092/callbacks"
      )

    val signature = request.header("Signature")
    // verify the signature with the public-key signer
    assertTrue(verifySignature(signature, "test_payload", "localhost:8092", signerPublic))
    assertFalse(verifySignature(signature, "test_payload_bad", "localhost:8092", random()))
  }
}
