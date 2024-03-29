package org.stellar.anchor

import javax.crypto.SecretKey
import org.stellar.anchor.auth.Sep10Jwt
import org.stellar.anchor.util.KeyUtil.toSecretKeySpecOrNull

class TestHelper {
  companion object {
    const val TEST_ACCOUNT = "GBJDSMTMG4YBP27ZILV665XBISBBNRP62YB7WZA2IQX2HIPK7ABLF4C2"
    const val TEST_MEMO = "123456"

    fun createSep10Jwt(
      account: String = TEST_ACCOUNT,
      accountMemo: String? = null,
      hostUrl: String = "",
      clientDomain: String = "vibrant.stellar.org",
      homeDomain: String = "test.stellar.org"
    ): Sep10Jwt {
      val issuedAt: Long = System.currentTimeMillis() / 1000L
      return Sep10Jwt.of(
        "$hostUrl/auth",
        if (accountMemo == null) account else "$account:$accountMemo",
        issuedAt,
        issuedAt + 60,
        "",
        clientDomain,
        homeDomain
      )
    }
  }
}

fun String.toSecretKey(): SecretKey {
  return toSecretKeySpecOrNull(this)
}
