package org.stellar.anchor.util

import com.google.common.io.BaseEncoding
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.io.IOException
import java.nio.charset.Charset
import java.security.KeyPairGenerator
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
import org.stellar.anchor.util.FileUtil.getResourceFileAsString
import org.stellar.anchor.util.RSAUtil.*

class RSAUtilTest {
  private val privateKeyString =
    "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAJPq9AU6f8GrkSccloRb+UJmbxilZ7iLkvp0FeR/yymQuJDMNXPXwt5MYR9VJoY0uMDHThaEwbQAc2M4L9wl0EtsESebODUTqnsWZ8/a6GsmvIM3kz02ZOUHLst0krSkECPUHLLUKuw/HWz3LWxWLeqVRIcsCwKJNaBPZJzFw/8PAgMBAAECgYBhWXSYLFQApmW1k/8LxWxa4wei9Nk6f8GPy+7Mn76Z8IFH6t4TC6FYpHQXJvdfxDsDxSgDcgP574IBfu0gulJHEIsoDy1xR8TPobfhwoS1NJn70a8KQA6Whu+K5pjJxGUC7bpJH4ZK0O+szB1mNC0WgUywIc0lXCFFnCrgbNFWQQJBAM7+zgHAyD6rqQem09JcabMkwxu0mYih1mvQ3uv/fay8BdgZAqMuacMAPQ/A1TLeuTbMQ0LojnK/QlotbhHVnSkCQQC27684POaRrVIsVo5uKvQsKSNlYPpvGrGapHKiVgIQYfZSEY9SrazqKbA5yux0OR8ZFFSiJCThRElpzhnWFYl3AkEArNsLnVsn3W3sUX93FAwoGHlylQhTzk2XiaF7BwjsIftBxhvcn/h6SWVBmI4ne7uSX7hj0tPxYNFmz3dwm2QPQQJBAKS8mPSy2wtqoiotVBvfcHzoGujrePpednuFBXosq7UnEpN7Hq7cmW9RVVHl7CMJYXjLNx/AHroBLX8rS1bflCcCQBclpUG1PybVy1jHXTdI0w6zB6AwjaeFN5x4+b7hRe29yLNF532uIatxif19LHb5jUC7EefpLWBxx/bB4JCIyug="
  private val publicKeyString =
    "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCT6vQFOn/Bq5EnHJaEW/lCZm8YpWe4i5L6dBXkf8spkLiQzDVz18LeTGEfVSaGNLjAx04WhMG0AHNjOC/cJdBLbBEnmzg1E6p7FmfP2uhrJryDN5M9NmTlBy7LdJK0pBAj1Byy1CrsPx1s9y1sVi3qlUSHLAsCiTWgT2ScxcP/DwIDAQAB"
  private val payload: String = "Test payload"
  private val privateKey = generatePrivateKey(privateKeyString)
  private val publicKey = generatePublicKey(publicKeyString)
  private val signature = sign(payload, privateKey)

  @Test
  fun `test valid public key`() {
    assertNotNull(generatePublicKey(publicKeyString, RSA_ALGORITHM))
    assertTrue(isValidPublicKey(publicKeyString, RSA_ALGORITHM))

    val publicKeyStr2 =
      publicKeyString
        .replace(BEGIN_PUBLIC_KEY, StringUtils.EMPTY)
        .replace(END_PUBLIC_KEY, StringUtils.EMPTY)
    assertNotNull(generatePublicKey(publicKeyStr2, RSA_ALGORITHM))
    assertTrue(isValidPublicKey(publicKeyStr2, RSA_ALGORITHM))
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

  @Test
  fun `test isSignatureValid() returns true for valid signature, eventObject, public key and algorithm`() {
    val publicKey: PublicKey = generatePublicKey(publicKeyString, RSA_ALGORITHM)
    val sig = sign(payload, privateKey)
    val validationResult = isValidSignature(signature, payload, publicKey)
    assertTrue(validationResult)
  }

  @Test
  fun `test isSignatureValid() for invalid signature`() {
    val publicKey: PublicKey = generatePublicKey(publicKeyString, RSA_ALGORITHM)
    val invalidSignature =
      "Yww6co109EfZ6BBam0zr1ewhv2gB3sFrfzcmbEFTttGp6GYVNEOslcMIMbjrFsFtkiEIO5ogvPI7Boz7yQUiXqh92Spj1aG5NoGDdjiW2ozTJxKq7ECK9IsS5vTjIxnBXUIXokCAN2BuiyA8d7LciJ6HwzS+DIvFNyvv7uKU6O0="
    assertFalse(isValidSignature(invalidSignature, payload, publicKey))
  }

  @Test
  fun `test isSignatureValid() for invalid data`() {
    val invalidEventObject = payload + "test"
    assertFalse(isValidSignature(signature, invalidEventObject, publicKey))
  }

  @Test
  fun `test isSignatureValid() for invalid public key`() {
    val invalidPublicKey: PublicKey = generatePublicKey(invalidPublicKey, RSA_ALGORITHM)
    assertFalse(isValidSignature(signature, payload, invalidPublicKey))
  }

  @Test
  fun `test isSignatureValid() for invalid algorithm`() {
    assertThrows<NoSuchAlgorithmException> {
      isValidSignature(signature, payload, publicKey, "INVALID_ALGORITHM")
    }
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

  @Test
  fun `test sign and verify with random key`() {
    val kp = KeyPairGenerator.getInstance("RSA").genKeyPair()
    val signature = sign(payload, kp.private)
    assertTrue(isValidSignature(signature, payload, kp.public))
  }

  @Test
  fun `test sign and verify`() {
    val privateKey = generatePrivateKey(privateKeyString)
    val publicKey = generatePublicKey(publicKeyString)

    val signature = sign(payload, privateKey)
    assertTrue(isValidSignature(signature, payload, publicKey))
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
