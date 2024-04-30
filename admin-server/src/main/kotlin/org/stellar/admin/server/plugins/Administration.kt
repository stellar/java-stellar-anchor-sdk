package org.stellar.admin.server.plugins

import io.ktor.server.application.*
import io.ktor.server.engine.*

/** Installs the ShutDownUrl plugin to the application to support remote shutdown. */
fun Application.configureAdministration() {
  install(ShutDownUrl.ApplicationCallPlugin) {
    // The URL that will be intercepted to shut down the admin server
    shutDownUrl = "/api/shutdown"
    exitCodeSupplier = { 0 }
  }
}
