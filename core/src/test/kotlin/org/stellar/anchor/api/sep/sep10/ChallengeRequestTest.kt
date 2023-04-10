package org.stellar.anchor.api.sep.sep10

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ChallengeRequestTest {
  companion object {
    const val TEST_ACCOUNT = "GCHLHDBOKG2JWMJQBTLSL5XG6NO7ESXI2TAQKZXCXWXB5WI2X6W233PR"
    const val TEST_MEMO = "test memo"
    const val TEST_HOME_DOMAIN = "test.stellar.org"
    const val TEST_CLIENT_DOMAIN = "test.client.stellar.org"
  }

  @Test
  fun `test ChallanegeRequest creation`() {
    val cr =
      ChallengeRequest.builder()
        .account(TEST_ACCOUNT)
        .memo(TEST_MEMO)
        .homeDomain(TEST_HOME_DOMAIN)
        .clientDomain(TEST_CLIENT_DOMAIN)
        .build()
    assertEquals(TEST_ACCOUNT, cr.account)
    assertEquals(TEST_MEMO, cr.memo)
    assertEquals(TEST_HOME_DOMAIN, cr.homeDomain)
    assertEquals(TEST_CLIENT_DOMAIN, cr.clientDomain)
  }
}
