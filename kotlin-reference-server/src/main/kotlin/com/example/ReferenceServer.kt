package com.example

import com.example.data.Config
import com.example.data.LocationConfig
import com.example.plugins.sep24
import com.example.plugins.testSep24
import com.example.sep24.*
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.addResourceSource
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
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

  embeddedServer(Netty, port = cfg.sep24.port) {
      install(ContentNegotiation) { json() }
      configureRouting(cfg)
      install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
      }
    }
    .start(args.getOrNull(0)?.toBooleanStrictOrNull() ?: true)
}

fun Application.configureRouting(cfg: Config) {
  routing {
    val helper = Sep24Helper(cfg)
    val deposit = DepositService(cfg)
    val withdrawal = WithdrawalService(cfg)

    sep24(helper, deposit, withdrawal, cfg.sep24.interactiveJwtKey)

    if (cfg.sep24.enableTest) {
      testSep24(helper, deposit, withdrawal, cfg.sep24.interactiveJwtKey)
    }
  }
}
