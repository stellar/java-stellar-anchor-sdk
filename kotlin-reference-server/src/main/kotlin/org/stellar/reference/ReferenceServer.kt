package org.stellar.reference

import com.sksamuel.hoplite.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import mu.KotlinLogging
import org.stellar.reference.data.Config
import org.stellar.reference.data.LocationConfig
import org.stellar.reference.plugins.*

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
    embeddedServer(Netty, port = cfg.appSettings.port) {
        install(ContentNegotiation) { json() }
        configureAuth(cfg)
        configureRouting(cfg)
        install(CORS) {
          anyHost()
          allowHeader(HttpHeaders.Authorization)
          allowHeader(HttpHeaders.ContentType)
        }
        install(RequestLoggerPlugin)
        install(RequestExceptionHandlerPlugin)
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
