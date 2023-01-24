package com.example

import com.example.data.Config
import com.example.plugins.*
import com.example.sep24.DepositService
import com.example.sep24.Sep24Helper
import com.example.sep24.WithdrawalService
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.addResourceSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import mu.KotlinLogging

val log = KotlinLogging.logger {}

fun main(args: Array<String>) {
  log.info { "Starting Kotlin reference server" }

  val cfg =
    ConfigLoaderBuilder.default()
      .addResourceSource("/config.yaml", optional = true)
      .addResourceSource("/default-config.yaml")
      .addPropertySource(PropertySource.environment())
      .build()
      .loadConfigOrThrow<Config>()

  embeddedServer(Netty, port = cfg.port) { configureRouting(cfg) }
    .start(args.getOrNull(0)?.toBooleanStrictOrNull() ?: true)
}

fun Application.configureRouting(cfg: Config) {
  routing { sep24(Sep24Helper(cfg), DepositService(cfg), WithdrawalService(cfg)) }
}
