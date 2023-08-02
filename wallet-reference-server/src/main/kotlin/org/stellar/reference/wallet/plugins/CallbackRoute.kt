package org.stellar.reference.wallet.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.reference.wallet.callback.CallbackEventService

fun Route.callback(CallbackEventService: CallbackEventService) {
  route("/callback") {
    // The `POST /callback` endpoint of the CallbackAPI to receive an event.
    post { call.respond("POST /callback") }
    get { call.respond("GET /callback ") }
  }
}
