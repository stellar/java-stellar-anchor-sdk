package org.stellar.anchor

import org.stellar.anchor.auth.Sep10Jwt

class TestHelper {
  companion object {
    const val TEST_ACCOUNT = "GBJDSMTMG4YBP27ZILV665XBISBBNRP62YB7WZA2IQX2HIPK7ABLF4C2"
    const val TEST_MEMO = "123456"

    fun createSep10Jwt(
      account: String = TEST_ACCOUNT,
      accountMemo: String? = null,
      hostUrl: String = "",
      clientDomain: String = "vibrant.stellar.org"
    ): Sep10Jwt {
      val issuedAt: Long = System.currentTimeMillis() / 1000L
      return Sep10Jwt.of(
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
