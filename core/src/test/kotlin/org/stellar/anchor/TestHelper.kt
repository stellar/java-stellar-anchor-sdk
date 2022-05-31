package org.stellar.anchor

import org.stellar.anchor.sep10.JwtToken

class TestHelper {
  companion object {
    private const val PUBLIC_KEY = "GBJDSMTMG4YBP27ZILV665XBISBBNRP62YB7WZA2IQX2HIPK7ABLF4C2"

    fun createJwtToken(
      publicKey: String = PUBLIC_KEY,
      hostUrl: String = "",
      clientDomain: String = "vibrant.stellar.org"
    ): JwtToken {
      val issuedAt: Long = System.currentTimeMillis() / 1000L
      return JwtToken.of("$hostUrl/auth", publicKey, issuedAt, issuedAt + 60, "", clientDomain)
    }
  }
}
