package org.stellar.reference

import com.sksamuel.hoplite.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.stellar.reference.data.Config
import org.stellar.reference.data.LocationConfig
import org.stellar.reference.event.EventService
import org.stellar.reference.plugins.event
import org.stellar.reference.plugins.sep24
import org.stellar.reference.plugins.testSep24
import org.stellar.reference.sep24.DepositService
import org.stellar.reference.sep24.Sep24Helper
import org.stellar.reference.sep24.WithdrawalService

val log = KotlinLogging.logger {}
lateinit var referenceKotlinSever: NettyApplicationEngine

fun main(args: Array<String>) {
  startServer(null, args.getOrNull(0)?.toBooleanStrictOrNull() ?: true)
}

fun startServer(envMap: Map<String, String>?, wait: Boolean) {
  log.info { "Starting Kotlin reference server" }

  // read config
  val cfg = readCfg(envMap)

  // start server
  referenceKotlinSever =
    embeddedServer(Netty, port = cfg.sep24.port) {
        install(ContentNegotiation) { json() }
        configureRouting(cfg)
        install(CORS) {
          anyHost()
          allowHeader(HttpHeaders.Authorization)
          allowHeader(HttpHeaders.ContentType)
        }
      }
      .start(wait)
}

fun readCfg(envMap: Map<String, String>?): Config {
  // Load location config
  val locationCfg =
    ConfigLoaderBuilder.default()
      .addPropertySource(PropertySource.environment())
      .build()
      .loadConfig<LocationConfig>()

  val cfgBuilder = ConfigLoaderBuilder.default()
  // Add environment variables as a property source.
  cfgBuilder.addPropertySource(PropertySource.environment())
  envMap?.run { cfgBuilder.addMapSource(this) }
  // Add config file as a property source if valid
  locationCfg.fold({}, { cfgBuilder.addFileSource(it.ktReferenceServerConfig) })
  // Add default config file as a property source.
  cfgBuilder.addResourceSource("/default-config.yaml")

  return cfgBuilder.build().loadConfigOrThrow<Config>()
}

fun stopServer() {
  log.info("Stopping Kotlin business reference server...")
  if (::referenceKotlinSever.isInitialized) (referenceKotlinSever).stop(5000, 30000)
  log.info("Kotlin reference server stopped...")
}

fun Application.configureRouting(cfg: Config) {
  routing {
    val helper = Sep24Helper(cfg)
    val depositService = DepositService(cfg)
    val withdrawalService = WithdrawalService(cfg)
    val eventService = EventService()

    sep24(helper, depositService, withdrawalService, cfg.sep24.interactiveJwtKey)
    event(eventService)

    if (cfg.sep24.enableTest) {
      testSep24(helper, depositService, withdrawalService, cfg.sep24.interactiveJwtKey)
    }
  }
}
