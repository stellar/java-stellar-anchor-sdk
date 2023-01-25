package com.example

import com.example.data.Config
import com.example.data.LocationConfig
import com.example.plugins.*
import com.example.sep24.DepositService
import com.example.sep24.Sep24Helper
import com.example.sep24.WithdrawalService
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.addResourceSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import mu.KotlinLogging

val log = KotlinLogging.logger {}

fun main(args: Array<String>) {
  log.info { "Starting Kotlin reference server" }

  // Support for configurable config location file.
  val locationCfg =
    ConfigLoaderBuilder.default()
      .addPropertySource(PropertySource.environment())
      .build()
      .loadConfig<LocationConfig>()

  val builder = ConfigLoaderBuilder.default()

  // If value is set, read config from the file.
  locationCfg.fold({}, { builder.addFileSource(it.ktReferenceServerConfig) })

  builder.addResourceSource("/default-config.yaml").addPropertySource(PropertySource.environment())

  val cfg = builder.build().loadConfigOrThrow<Config>()

  embeddedServer(Netty, port = cfg.sep24.port) { configureRouting(cfg) }
    .start(args.getOrNull(0)?.toBooleanStrictOrNull() ?: true)
}

fun Application.configureRouting(cfg: Config) {
  routing { sep24(Sep24Helper(cfg), DepositService(cfg), WithdrawalService(cfg)) }
}
