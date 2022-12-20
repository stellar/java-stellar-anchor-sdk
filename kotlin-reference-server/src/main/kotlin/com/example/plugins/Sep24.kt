package com.example.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging

val log = KotlinLogging.logger {}

fun Route.sep24() {
  route("/sep24/interactive") {
    get {
      log.info("Called /sep24/interactive with parameters ${call.parameters}")

      val operation =
        call.parameters["operation"]
          ?: return@get call.respondText(
            "Missing operation parameter",
            status = HttpStatusCode.BadRequest
          )

      when (operation.lowercase()) {
        "deposit" -> call.respondText("The sep24 interactive DEPOSIT starts here.")
        "withdraw" -> call.respondText("The sep24 interactive WITHDRAW starts here.")
        else ->
          call.respondText(
            "The only supported operations are \"deposit\" or \"withdraw\"",
            status = HttpStatusCode.BadRequest
          )
      }
    }
  }
}
