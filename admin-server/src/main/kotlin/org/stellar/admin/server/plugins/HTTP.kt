package org.stellar.admin.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.routing.*

/** Configures CORS (Cross Origin Resource Sharing) and OpenAPI settings for the application. */
fun Application.configureHTTP() {
  install(CORS) {
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowHeader(HttpHeaders.Authorization)
    anyHost()
  }
  routing { openAPI(path = "openapi") }
}
