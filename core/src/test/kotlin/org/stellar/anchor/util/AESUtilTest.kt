package org.stellar.anchor.util

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

internal class AESUtilTest {
  @Test
  fun `test encrypt decrypt`() {
    val secret = "secret"
    val salt = "salt"
    val string = "test"

    val aes1 = AESUtil(secret, salt)
    val aes2 = AESUtil(secret, salt)
    val iv = aes1.iv

    val result = aes2.decrypt(aes1.encrypt(string, iv), iv)

    assertEquals(string, result)
  }
}
