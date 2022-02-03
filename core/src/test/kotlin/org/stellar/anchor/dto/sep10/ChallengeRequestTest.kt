package org.stellar.anchor.dto.sep10

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ChallengeRequestTest {
  companion object {
    const val TEST_ACCOUNT = "GCUZ6YLL5RQBTYLTTQLPCM73C5XAIUGK2TIMWQH7HPSGWVS2KJ2F3CHS"
    const val TEST_MEMO = "test memo"
    const val TEST_HOME_DOMAIN = "test.stellar.org"
    const val TEST_CLIENT_DOMAIN = "test.client.stellar.org"
  }

  @Test
  fun of() {
    val cr = ChallengeRequest.of(TEST_ACCOUNT, TEST_MEMO, TEST_HOME_DOMAIN, TEST_CLIENT_DOMAIN)
    assertEquals(TEST_ACCOUNT, cr.account)
    assertEquals(TEST_MEMO, cr.memo)
    assertEquals(TEST_HOME_DOMAIN, cr.homeDomain)
    assertEquals(TEST_CLIENT_DOMAIN, cr.clientDomain)
  }
}
