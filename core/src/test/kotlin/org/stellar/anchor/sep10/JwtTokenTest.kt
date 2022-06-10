package org.stellar.anchor.sep10

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.stellar.anchor.Constants.Companion.TEST_ACCOUNT
import org.stellar.anchor.Constants.Companion.TEST_CLIENT_DOMAIN

class JwtTokenTest {
  private val issuedAt = Instant.now().epochSecond
  private val expiresAt = issuedAt + 500

  @Test
  fun of() {
    val token = JwtToken.of("iss", TEST_ACCOUNT, issuedAt, expiresAt, "", TEST_CLIENT_DOMAIN)
    assertEquals(TEST_ACCOUNT, token.account)
    assertEquals(TEST_CLIENT_DOMAIN, token.getClientDomain())
    assertEquals(issuedAt, token.getIat())
    assertEquals(expiresAt, token.getExp())
    assertEquals(TEST_CLIENT_DOMAIN, token.getClientDomain())
  }
  @Test
  fun of_accountMemo() {
    val accountMemo = "135689"
    val token =
      JwtToken.of("iss", "$TEST_ACCOUNT:$accountMemo", issuedAt, expiresAt, "", TEST_CLIENT_DOMAIN)
    assertEquals(TEST_ACCOUNT, token.account)
    assertEquals(accountMemo, token.accountMemo)
    assertEquals(TEST_CLIENT_DOMAIN, token.getClientDomain())
    assertEquals(issuedAt, token.getIat())
    assertEquals(expiresAt, token.getExp())
    assertEquals(TEST_CLIENT_DOMAIN, token.getClientDomain())
  }
}
