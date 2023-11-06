package org.stellar.anchor.client.util

import com.google.common.io.BaseEncoding
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.io.IOException
import java.nio.charset.Charset
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import kotlin.test.assertNotNull
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.platform.utils.RSAUtil.*
import org.stellar.anchor.util.FileUtil.getResourceFileAsString

class RSAUtilTest {

  private val signature: String =
    getResourceFileAsString("custody/fireblocks/webhook/submitted_event_valid_signature.txt")
  private val eventObject: String =
    getCompactJsonString("custody/fireblocks/webhook/submitted_event_request.json")

  @ParameterizedTest
  @ValueSource(strings = ["custody/fireblocks/webhook/public_key.txt"])
  fun `test valid public key`(fileName: String) {
    var publicKey: String = getResourceFileAsString(fileName)
    assertNotNull(generatePublicKey(publicKey, RSA_ALGORITHM))
    assertTrue(isValidPublicKey(publicKey, RSA_ALGORITHM))

    publicKey =
      publicKey
        .replace(BEGIN_PUBLIC_KEY, StringUtils.EMPTY)
        .replace(END_PUBLIC_KEY, StringUtils.EMPTY)
    assertNotNull(generatePublicKey(publicKey, RSA_ALGORITHM))
    assertTrue(isValidPublicKey(publicKey, RSA_ALGORITHM))
  }

  @ParameterizedTest
  @ValueSource(strings = [""])
  fun `test empty public key`(publicKey: String) {
    assertThrows<InvalidKeySpecException> { generatePublicKey(publicKey, RSA_ALGORITHM) }
    assertFalse(isValidPublicKey(publicKey, RSA_ALGORITHM))
  }

  @ParameterizedTest
  @ValueSource(strings = ["test_public_key"])
  fun `test invalid public key`(publicKey: String) {
    val decodedPublicKey =
      BaseEncoding.base64().encode(publicKey.toByteArray(Charset.defaultCharset())).toString()
    assertThrows<InvalidKeySpecException> { generatePublicKey(decodedPublicKey, RSA_ALGORITHM) }
    assertFalse(isValidPublicKey(decodedPublicKey, RSA_ALGORITHM))
  }

  @ParameterizedTest
  @ValueSource(strings = ["test_public_key"])
  fun `test invalid public key encoding`(publicKey: String) {
    val decodedPublicKey = BaseEncoding.base32().encode(publicKey.toByteArray())
    assertThrows<InvalidKeySpecException> { generatePublicKey(decodedPublicKey, RSA_ALGORITHM) }
    assertFalse(isValidPublicKey(decodedPublicKey, RSA_ALGORITHM))
  }

  @ParameterizedTest
  @ValueSource(strings = [""])
  fun `test invalid public key algorithm`(publicKey: String) {
    assertThrows<NoSuchAlgorithmException> { generatePublicKey(publicKey, "INVALID_ALGORITHM") }
    assertFalse(isValidPublicKey(publicKey, RSA_ALGORITHM))
  }

  @ParameterizedTest
  @ValueSource(strings = ["custody/fireblocks/webhook/public_key.txt"])
  fun `test isSignatureValid() returns true for valid signature, eventObject, public key and algorithm`(
    fileName: String
  ) {
    val publicKeyString: String = getResourceFileAsString(fileName)
    val publicKey: PublicKey = generatePublicKey(publicKeyString, RSA_ALGORITHM)
    val validationResult =
      isValidSignature(signature, eventObject, publicKey, SHA512_WITH_RSA_ALGORITHM)
    assertTrue(validationResult)
  }

  @ParameterizedTest
  @ValueSource(strings = ["custody/fireblocks/webhook/public_key.txt"])
  fun `test isSignatureValid() for invalid signature`(fileName: String) {
    val publicKeyString: String = getResourceFileAsString(fileName)
    val publicKey: PublicKey = generatePublicKey(publicKeyString, RSA_ALGORITHM)
    val invalidSignature =
      getResourceFileAsString("custody/fireblocks/webhook/submitted_event_invalid_signature.txt")
    assertFalse(
      isValidSignature(invalidSignature, eventObject, publicKey, SHA512_WITH_RSA_ALGORITHM)
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["custody/fireblocks/webhook/public_key.txt"])
  fun `test isSignatureValid() for invalid data`(fileName: String) {
    val publicKeyString: String = getResourceFileAsString(fileName)
    val publicKey: PublicKey = generatePublicKey(publicKeyString, RSA_ALGORITHM)
    val invalidEventObject = eventObject + "test"
    assertFalse(
      isValidSignature(signature, invalidEventObject, publicKey, SHA512_WITH_RSA_ALGORITHM)
    )
  }

  @Test
  fun `test isSignatureValid() for invalid public key`() {
    val invalidPublicKey: PublicKey = generatePublicKey(invalidPublicKey, RSA_ALGORITHM)
    assertFalse(
      isValidSignature(signature, eventObject, invalidPublicKey, SHA512_WITH_RSA_ALGORITHM)
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["custody/fireblocks/webhook/public_key.txt"])
  fun `test isSignatureValid() for invalid algorithm`(fileName: String) {
    val publicKeyString: String = getResourceFileAsString(fileName)
    val publicKey: PublicKey = generatePublicKey(publicKeyString, RSA_ALGORITHM)
    assertThrows<NoSuchAlgorithmException> {
      isValidSignature(signature, eventObject, publicKey, "INVALID_ALGORITHM")
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["custody/fireblocks/client/secret_key.txt"])
  fun `test valid private key`(fileName: String) {
    var privateKey: String = getResourceFileAsString(fileName)
    assertNotNull(generatePrivateKey(privateKey, RSA_ALGORITHM))
    assertTrue(isValidPrivateKey(privateKey, RSA_ALGORITHM))

    privateKey =
      privateKey
        .replace(BEGIN_PUBLIC_KEY, StringUtils.EMPTY)
        .replace(END_PUBLIC_KEY, StringUtils.EMPTY)
    assertNotNull(generatePrivateKey(privateKey, RSA_ALGORITHM))
    assertTrue(isValidPrivateKey(privateKey, RSA_ALGORITHM))
  }

  @ParameterizedTest
  @ValueSource(strings = [""])
  fun `test empty private key`(privateKey: String) {
    assertThrows<InvalidKeySpecException> { generatePrivateKey(privateKey, RSA_ALGORITHM) }
    assertFalse(isValidPrivateKey(privateKey, RSA_ALGORITHM))
  }

  @ParameterizedTest
  @ValueSource(strings = ["test_public_key"])
  fun `test invalid private key`(privateKey: String) {
    val decodedPrivateKey =
      BaseEncoding.base64().encode(privateKey.toByteArray(Charset.defaultCharset())).toString()
    assertThrows<InvalidKeySpecException> { generatePrivateKey(decodedPrivateKey, RSA_ALGORITHM) }
    assertFalse(isValidPrivateKey(decodedPrivateKey, RSA_ALGORITHM))
  }

  @ParameterizedTest
  @ValueSource(strings = ["test_public_key"])
  fun `test invalid private key encoding`(privateKey: String) {
    val decodedPrivateKey = BaseEncoding.base32().encode(privateKey.toByteArray())
    assertThrows<InvalidKeySpecException> { generatePrivateKey(decodedPrivateKey, RSA_ALGORITHM) }
    assertFalse(isValidPublicKey(decodedPrivateKey, RSA_ALGORITHM))
  }

  @ParameterizedTest
  @ValueSource(strings = [""])
  fun `test invalid private key algorithm`(privateKey: String) {
    assertThrows<NoSuchAlgorithmException> { generatePrivateKey(privateKey, "INVALID_ALGORITHM") }
    assertFalse(isValidPublicKey(privateKey, RSA_ALGORITHM))
  }

  @Throws(IOException::class, SepNotFoundException::class)
  fun getCompactJsonString(fileName: String): String {
    val gson = GsonBuilder().serializeNulls().create()
    val el = JsonParser.parseString(getResourceFileAsString(fileName))
    return gson.toJson(el)
  }

  private val invalidPublicKey =
    """-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCpEk+ZuHkZdeSktVTV3MoYPcpI
+3EPVueKsk4cEeAn2XFgBWLaqz8KOcaMK1/mL92rTAPBaznxvhFL6DKf1C9EKjGf
bHFsXrGLEduo7Puk6L4H2YHg+Tpyjaa7bAIs2OjZe2rhx6Qmkk1tiuyJLMmKeJxO
E+UUh8VtHq82whkX0wIDAQAB
-----END PUBLIC KEY-----"""
}
