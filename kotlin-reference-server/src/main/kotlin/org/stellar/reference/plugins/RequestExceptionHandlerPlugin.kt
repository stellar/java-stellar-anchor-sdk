package org.stellar.reference.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.response.*
import org.stellar.reference.callbacks.BadRequestException
import org.stellar.reference.callbacks.NotFoundException
import org.stellar.reference.callbacks.UnprocessableEntityException
import org.stellar.reference.log

val RequestExceptionHandlerPlugin =
  createApplicationPlugin(name = "RequestExceptionHandlerPlugin") {
    on(CallFailed) { call, throwable ->
      when (throwable) {
        is BadRequestException -> call.respond(HttpStatusCode.BadRequest, throwable)
        is NotFoundException -> call.respond(HttpStatusCode.NotFound, throwable)
        is UnprocessableEntityException ->
          call.respond(HttpStatusCode.UnprocessableEntity, throwable)
        else -> {
          log.error("Unexpected exception", throwable)
          call.respond(HttpStatusCode.InternalServerError)
        }
      }
    }
  }
