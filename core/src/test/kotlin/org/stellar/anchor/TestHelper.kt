package org.stellar.anchor

import org.stellar.anchor.sep10.JwtToken

class TestHelper {
  companion object {
    private const val TEST_ACCOUNT = "GBJDSMTMG4YBP27ZILV665XBISBBNRP62YB7WZA2IQX2HIPK7ABLF4C2"

    fun createJwtToken(
      account: String = TEST_ACCOUNT,
      accountMemo: String? = null,
      hostUrl: String = "",
      clientDomain: String = "vibrant.stellar.org"
    ): JwtToken {
      val issuedAt: Long = System.currentTimeMillis() / 1000L
      return JwtToken.of(
        "$hostUrl/auth",
        if (accountMemo == null) account else "$account:$accountMemo",
        issuedAt,
        issuedAt + 60,
        "",
        clientDomain
      )
    }
  }
}
