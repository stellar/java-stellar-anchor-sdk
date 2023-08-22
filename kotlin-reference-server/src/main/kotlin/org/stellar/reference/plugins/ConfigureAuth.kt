package org.stellar.reference.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.stellar.reference.data.Config
import org.stellar.reference.data.IntegrationAuth

fun Application.configureAuth(cfg: Config) {
  when (cfg.integrationAuth.authType) {
    IntegrationAuth.Type.JWT ->
      authentication {
        jwt("integration-auth") {
          verifier(
            JWT.require(Algorithm.HMAC256(cfg.integrationAuth.platformToAnchorSecret)).build()
          )
          validate { credential ->
            val principal = JWTPrincipal(credential.payload)
            if (principal.payload.expiresAt.time < System.currentTimeMillis()) {
              null
            } else {
              principal
            }
          }
          challenge { _, _ ->
            call.respond(HttpStatusCode.Unauthorized, "Token is invalid or expired")
          }
        }
      }
    IntegrationAuth.Type.API_KEY -> {
      TODO("API key auth not implemented yet")
    }
    IntegrationAuth.Type.NONE -> {
      log.warn("Authentication is disabled. Endpoints are not secured.")
      authentication { basic("integration-auth") { skipWhen { true } } }
    }
  }
}
