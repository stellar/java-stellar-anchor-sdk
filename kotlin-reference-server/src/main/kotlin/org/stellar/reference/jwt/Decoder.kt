package org.stellar.reference.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import javax.crypto.spec.SecretKeySpec
import org.stellar.reference.data.JwtToken

object JwtDecoder {
  fun decode(cipher: String, jwtKey: String): JwtToken {
    val secretKeySpec = SecretKeySpec(jwtKey.toByteArray(), Jwts.SIG.HS256.id)
    val jwt = Jwts.parser().verifyWith(secretKeySpec).build().parseSignedClaims(cipher)

    val claims: Claims = jwt.payload

    @Suppress("UNCHECKED_CAST")
    return JwtToken(
      claims["jti"] as String,
      claims["exp"].toString().toLong(),
      claims["data"] as Map<String, String>,
    )
  }
}
