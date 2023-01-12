package com.example

import com.example.plugins.*
import com.example.sep24.DepositService
import com.example.sep24.WithdrawalService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import mu.KotlinLogging

const val DEFAULT_KOTLIN_REFERENCE_SERVER_PORT = 8091

val log = KotlinLogging.logger {}

// TODO: make configurable
val depositService = DepositService()
val withdrawalService = WithdrawalService()

fun main(args: Array<String>) {
  log.info { "Starting Kotlin reference server" }

  embeddedServer(
      Netty,
      port = args.getOrNull(0)?.toIntOrNull() ?: DEFAULT_KOTLIN_REFERENCE_SERVER_PORT
    ) {
      configureRouting()
    }
    .start(args.getOrNull(1)?.toBooleanStrictOrNull() ?: true)
}

fun Application.configureRouting() {
  routing { sep24(depositService, withdrawalService) }
}
