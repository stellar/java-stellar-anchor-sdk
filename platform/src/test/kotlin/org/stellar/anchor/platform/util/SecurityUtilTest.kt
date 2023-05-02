package org.stellar.anchor.platform.util

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
import org.stellar.anchor.platform.utils.SecurityUtil.BEGIN_PUBLIC_KEY
import org.stellar.anchor.platform.utils.SecurityUtil.END_PUBLIC_KEY
import org.stellar.anchor.platform.utils.SecurityUtil.RSA_ALGORITHM
import org.stellar.anchor.platform.utils.SecurityUtil.SHA512_WITH_RSA_ALGORITHM
import org.stellar.anchor.platform.utils.SecurityUtil.generatePublicKey
import org.stellar.anchor.platform.utils.SecurityUtil.isValidPublicKey
import org.stellar.anchor.platform.utils.SecurityUtil.isValidSignature
import org.stellar.anchor.util.FileUtil.getResourceFileAsString

class SecurityUtilTest {

  private val signature: String =
    getResourceFileAsString("custody/api/webhook/fireblocks/signature.txt")
  private val eventObject: String =
    getCompactJsonString("custody/api/webhook/fireblocks/webhook_request.json")

  @ParameterizedTest
  @ValueSource(strings = ["custody/api/webhook/fireblocks/public_key.txt"])
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
  fun `test invalid algorithm`(publicKey: String) {
    assertThrows<NoSuchAlgorithmException> { generatePublicKey(publicKey, "INVALID_ALGORITHM") }
    assertFalse(isValidPublicKey(publicKey, RSA_ALGORITHM))
  }

  @ParameterizedTest
  @ValueSource(strings = ["custody/api/webhook/fireblocks/public_key.txt"])
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
  @ValueSource(strings = ["custody/api/webhook/fireblocks/public_key.txt"])
  fun `test isSignatureValid() for invalid signature`(fileName: String) {
    val publicKeyString: String = getResourceFileAsString(fileName)
    val publicKey: PublicKey = generatePublicKey(publicKeyString, RSA_ALGORITHM)
    val invalidSignature =
      "Yww6co109EfZ6BBam0zr1ewhv2gB3sFrfzcmbEFTttGp6GYVNEOslcMIMbjrFsFtkiEIO5ogvPI7Boz7y" +
        "QUiXqh92Spj1aG5NoGDdjiW2ozTJxKq7ECK9IsS5vTjIxnBXUIXokCAN2BuiyA8d7LciJ6HwzS+DIvFNyvv7uKU6O0="
    assertFalse(
      isValidSignature(invalidSignature, eventObject, publicKey, SHA512_WITH_RSA_ALGORITHM)
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["custody/api/webhook/fireblocks/public_key.txt"])
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
    val publicKeyString =
      "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCx8LacAqPnX63Bj4ayc" +
        "DQWZij329nXh2oL/XFXbvZpFcEXrIB1O42rHQ93Ubnul7YmgGpArBfm6Kkotl48kRD/5iv6xuHcgaK" +
        "G+IQDRiR63XKcYKovWbdzi51F/DMaj2PESj10WoWzsFhPyUNrXG2QEi/1H+4muP3dbo2DT80FIQIDAQAB"
    val invalidPublicKey: PublicKey = generatePublicKey(publicKeyString, RSA_ALGORITHM)
    assertFalse(
      isValidSignature(signature, eventObject, invalidPublicKey, SHA512_WITH_RSA_ALGORITHM)
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["custody/api/webhook/fireblocks/public_key.txt"])
  fun `test isSignatureValid() for invalid algorithm`(fileName: String) {
    val publicKeyString: String = getResourceFileAsString(fileName)
    val publicKey: PublicKey = generatePublicKey(publicKeyString, RSA_ALGORITHM)
    assertThrows<NoSuchAlgorithmException> {
      isValidSignature(signature, eventObject, publicKey, "INVALID_ALGORITHM")
    }
  }

  @Throws(IOException::class, SepNotFoundException::class)
  fun getCompactJsonString(fileName: String): String {
    val gson = GsonBuilder().serializeNulls().create()
    val el = JsonParser.parseString(getResourceFileAsString(fileName))
    return gson.toJson(el)
  }
}
