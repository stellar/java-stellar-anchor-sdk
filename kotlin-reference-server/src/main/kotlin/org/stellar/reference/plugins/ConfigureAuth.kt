package org.stellar.reference.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.stellar.reference.data.AuthSettings
import org.stellar.reference.data.Config

const val AUTH_CONFIG_ENDPOINT = "endpoint-auth"

fun Application.configureAuth(cfg: Config) {
  when (cfg.authSettings.type) {
    AuthSettings.Type.JWT ->
      authentication {
        jwt(AUTH_CONFIG_ENDPOINT) {
          verifier(JWT.require(Algorithm.HMAC256(cfg.authSettings.platformToAnchorSecret)).build())
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
    AuthSettings.Type.API_KEY -> {
      TODO("API key auth not implemented yet")
    }
    AuthSettings.Type.NONE -> {
      log.warn("Authentication is disabled. Endpoints are not secured.")
      authentication { basic(AUTH_CONFIG_ENDPOINT) { skipWhen { true } } }
    }
  }
}
