package org.stellar.admin.server.plugins

import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** Function to configure the serialization of the application. */
fun Application.configureSerialization() {
  install(ContentNegotiation) { gson {} }
  routing {
    get("/json/gson") {
      call.respond(mapOf("Example" to "This is the placeholder for content negotiation"))
    }
  }
}
