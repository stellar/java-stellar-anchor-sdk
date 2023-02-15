package com.example.jwt

import com.example.data.JwtToken
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.impl.DefaultJwsHeader

object JwtDecoder {
  fun decode(cipher: String, jwtKey: String): JwtToken {
    val jwtParser: JwtParser = Jwts.parser()
    jwtParser.setSigningKey(jwtKey.toByteArray())
    // Will throw exception if key is invalid
    val jwt = jwtParser.parseClaimsJws(cipher)
    val header = jwt.header

    require(header is DefaultJwsHeader) {
      // This should not happen
      "Bad token"
    }

    val defaultHeader: DefaultJwsHeader = header

    require(defaultHeader.algorithm == io.jsonwebtoken.SignatureAlgorithm.HS256.value) {
      // Not signed by the JWTService.
      "Bad token"
    }

    val claims: Claims = jwt.body

    return JwtToken(
      (claims["data"] as Map<*, *>)["transaction_id"] as String,
      claims["exp"].toString().toLong(),
    )
  }
}
