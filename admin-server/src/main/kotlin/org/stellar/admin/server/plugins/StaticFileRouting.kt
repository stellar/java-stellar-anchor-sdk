package org.stellar.admin.server.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

/** Function to configure the static files routing of the application. */
fun Application.configureStaticFilesRouting() {
  routing {
    // The UI files are served from the /ui path.
    staticResources("/ui", "ui")
  }
}
