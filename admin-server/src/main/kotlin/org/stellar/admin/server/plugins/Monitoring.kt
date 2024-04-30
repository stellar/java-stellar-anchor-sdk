package org.stellar.admin.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

/** Enables request logging for the application. */
fun Application.configureMonitoring() {
  install(CallLogging) {
    level = Level.INFO
    filter { call -> call.request.path().startsWith("/") }
  }
}
