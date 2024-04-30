package org.stellar.admin.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.admin.server.config.RootConfig

fun Application.configureRouting() {
  routing {
    get("/") { call.respondRedirect("/ui/index.html") }
    get("/configurations") { call.respond(RootConfig()) }
    patch("/configurations") { call.respond(RootConfig()) }
  }
}
