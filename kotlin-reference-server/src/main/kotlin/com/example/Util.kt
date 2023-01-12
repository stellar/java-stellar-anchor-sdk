package com.example

import io.ktor.server.application.*

fun ApplicationCall.safelyGet(paramName: String): String {
  return this.parameters[paramName]
    ?: throw ClientException("Missing $paramName parameter in the request")
}

class ClientException(message: String) : Exception(message)
