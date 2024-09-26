package org.stellar.anchor.platform.integrationtest

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.Sep10Jwt
import org.stellar.anchor.platform.AbstractIntegrationTests
import org.stellar.anchor.platform.SMART_WALLET_ADDRESS
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Log

class Sep10CTests : AbstractIntegrationTests(TestConfig()) {
  private val jwtService =
    JwtService(
      config.env["secret.sep6.more_info_url.jwt_secret"],
      config.env["secret.sep10.jwt_secret"]!!,
      config.env["secret.sep24.interactive_url.jwt_secret"]!!,
      config.env["secret.sep24.more_info_url.jwt_secret"]!!,
      config.env["secret.callback_api.auth_secret"]!!,
      config.env["secret.platform_api.auth_secret"]!!,
      null,
    )

  private var webAuthDomain = toml.getString("WEB_AUTH_ENDPOINT_C")

  @Test
  fun testChallengeSigning() {
    val challenge =
      sep10CClient.getChallenge(
        ChallengeRequest.builder()
          .address(SMART_WALLET_ADDRESS)
          .memo("123")
          .homeDomain(webAuthDomain)
          .clientDomain("example.com")
          .build()
      )
    Log.info("authorizationEntry: ${challenge.authorizationEntry}")
    Log.info("signature: ${challenge.serverSignature}")

    val validationRequest = sep10CClient.sign(challenge)
    Log.info("credentials: ${GsonUtils.getInstance().toJson(validationRequest.credentials)}")

    val validationResponse = sep10CClient.validate(validationRequest)
    Log.info("token: ${validationResponse.token}")

    val jwt = jwtService.decode(validationResponse.token, Sep10Jwt::class.java)
    Log.info("jwt: ${GsonUtils.getInstance().toJson(jwt)}")

    assertEquals("example.com", jwt.clientDomain)
    assertEquals(webAuthDomain, jwt.homeDomain)
    assertEquals(SMART_WALLET_ADDRESS, jwt.account)
    assertEquals("123", jwt.accountMemo)
    assertNotNull(jwt.jti)
    assertEquals(webAuthDomain, jwt.iss)
    assertEquals("${SMART_WALLET_ADDRESS}:123", jwt.sub)
    assertNotNull(jwt.issuedAt)
    assertNotNull(jwt.expiresAt)
  }
}
