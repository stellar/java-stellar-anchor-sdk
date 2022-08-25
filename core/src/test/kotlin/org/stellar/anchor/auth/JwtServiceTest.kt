package org.stellar.anchor.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.mockk.every
import io.mockk.mockk
import java.nio.charset.StandardCharsets
import java.util.*
import org.apache.commons.codec.binary.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.config.AppConfig

internal class JwtServiceTest {
  companion object {
    const val TEST_ISS = "test_issuer"
    const val TEST_SUB = "test_sub"
    val TEST_IAT = System.currentTimeMillis() / 1000
    val TEST_EXP = System.currentTimeMillis() / 1000 + 900
    const val TEST_JTI = "test_jti"
    const val TEST_CLIENT_DOMAIN = "test_client_domain"
  }

  @Test
  fun `test apply JWT encoding and decoding and make sure the original values are not changed`() {
    val appConfig = mockk<AppConfig>()
    every { appConfig.jwtSecretKey } returns "jwt_secret"

    val jwtService = JwtService(appConfig)
    val token =
      JwtToken.of(
        TEST_ISS,
        TEST_SUB,
        TEST_IAT,
        TEST_EXP,
        TEST_JTI,
        TEST_CLIENT_DOMAIN,
      )
    val cipher = jwtService.encode(token)
    val dt = jwtService.decode(cipher)

    assertEquals(dt.iss, token.iss)
    assertEquals(dt.sub, token.sub)
    assertEquals(dt.iat, token.iat)
    assertEquals(dt.exp, token.exp)
    assertEquals(dt.jti, token.jti)
    assertEquals(dt.clientDomain, token.clientDomain)
    assertEquals(dt.account, token.sub)
    assertEquals(dt.transactionId, token.jti)
    assertEquals(dt.issuer, token.iss)
    assertEquals(dt.issuedAt, token.iat)
    assertEquals(dt.expiresAt, token.exp)
  }

  @Test
  fun `make sure decoding bad cipher test throws an error`() {
    val appConfig = mockk<AppConfig>()
    every { appConfig.jwtSecretKey } returns "jwt_secret"

    val jwtService = JwtService(appConfig)

    assertThrows<MalformedJwtException> { jwtService.decode("This is a bad cipher") }
  }

  @Test
  fun `make sure JwtService only decodes HS256`() {
    val appConfig = mockk<AppConfig>()
    every { appConfig.jwtSecretKey } returns "jwt_secret"

    val jwtService = JwtService(appConfig)
    val jwtKey =
      Base64.encodeBase64String(appConfig.jwtSecretKey.toByteArray(StandardCharsets.UTF_8))

    val builder =
      Jwts.builder()
        .setId("mock_id")
        .setIssuer(TEST_ISS)
        .setSubject(TEST_SUB)
        .setIssuedAt(Date(System.currentTimeMillis()))
        .setExpiration(Date(System.currentTimeMillis() + 300000))

    var token = builder.signWith(SignatureAlgorithm.HS256, jwtKey).compact()
    jwtService.decode(token)

    token = builder.signWith(SignatureAlgorithm.HS384, jwtKey).compact()
    assertThrows<IllegalArgumentException> { jwtService.decode(token) }

    token = builder.signWith(SignatureAlgorithm.HS512, jwtKey).compact()
    assertThrows<IllegalArgumentException> { jwtService.decode(token) }
  }
}
