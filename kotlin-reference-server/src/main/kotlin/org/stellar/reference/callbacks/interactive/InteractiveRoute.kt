package org.stellar.reference.callbacks.interactive

import io.ktor.http.ContentType.Text.Html
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.sep24Interactive() {
  route("/sep24/transaction/interactive") {
    get { call.respondText("The sep24 interactive flow starts here.", Html) }
  }
  route("/sep24/transaction/more_info") {
    get { call.respondText("The sep24 more_info endpoint starts here.", Html) }
  }
}
