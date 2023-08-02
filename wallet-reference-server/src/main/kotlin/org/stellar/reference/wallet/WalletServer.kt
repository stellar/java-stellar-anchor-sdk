package org.stellar.reference.wallet

import com.sksamuel.hoplite.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.stellar.reference.wallet.callback.CallbackEventService
import org.stellar.reference.wallet.data.Config
import org.stellar.reference.wallet.data.LocationConfig
import org.stellar.reference.wallet.plugins.callback

val log = KotlinLogging.logger {}
lateinit var walletServer: NettyApplicationEngine

fun main(args: Array<String>) {
  startServer(null, args.getOrNull(0)?.toBooleanStrictOrNull() ?: true)
}

fun startServer(envMap: Map<String, String>?, wait: Boolean) {
  log.info { "Starting wallet reference server" }

  // read config
  val cfg = readCfg(envMap)

  // start server
  walletServer =
    embeddedServer(Netty, port = cfg.port) {
        install(ContentNegotiation) { json() }
        install(CallLogging)
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
  locationCfg.fold({}, { cfgBuilder.addFileSource(it.walletServerConfig) })
  // Add default config file as a property source.
  cfgBuilder.addResourceSource("/wallet-server-config.yaml")

  return cfgBuilder.build().loadConfigOrThrow<Config>()
}

fun stopServer() {
  if (::walletServer.isInitialized) (walletServer).stop(1000, 1000)
}

fun Application.configureRouting(cfg: Config) {
  routing { callback(CallbackEventService()) }
}
