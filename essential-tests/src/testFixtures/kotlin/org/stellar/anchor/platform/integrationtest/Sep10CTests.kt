package org.stellar.anchor.platform.integrationtest

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.Sep10Jwt
import org.stellar.anchor.client.Sep10CClient
import org.stellar.anchor.platform.AbstractIntegrationTests
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Log
import org.stellar.sdk.SorobanServer

class Sep10CTests : AbstractIntegrationTests(TestConfig()) {
  private var sep10CClient: Sep10CClient =
    Sep10CClient(
      toml.getString("WEB_AUTH_ENDPOINT_C"),
      toml.getString("SIGNING_KEY"),
      SorobanServer("https://soroban-testnet.stellar.org"),
    )
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
  private var clientWalletContractAddress =
    "CDYOQJLKZWHZ2CVN43EVEQNDLEN544IGCO5A52UG4YS6KDN5QQ2LUWKY"

  @Test
  fun testChallengeSigning() {
    val challenge =
      sep10CClient.getChallenge(
        ChallengeRequest.builder()
          .address(clientWalletContractAddress)
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
    assertEquals(clientWalletContractAddress, jwt.account)
    assertEquals("123", jwt.accountMemo)
    assertNotNull(jwt.jti)
    assertEquals("https://localhost:8080/c/auth", jwt.iss)
    assertEquals("${clientWalletContractAddress}:123", jwt.sub)
    assertNotNull(jwt.issuedAt)
    assertNotNull(jwt.expiresAt)
  }
}
