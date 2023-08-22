package org.stellar.reference

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.stellar.reference.dao.JdbcCustomerRepository
import org.stellar.reference.dao.JdbcQuoteRepository
import org.stellar.reference.data.Config
import org.stellar.reference.data.IntegrationAuth
import org.stellar.reference.data.LocationConfig
import org.stellar.reference.event.EventService
import org.stellar.reference.event.event
import org.stellar.reference.integration.customer.CustomerService
import org.stellar.reference.integration.customer.customer
import org.stellar.reference.integration.fee.FeeService
import org.stellar.reference.integration.fee.fee
import org.stellar.reference.integration.rate.RateService
import org.stellar.reference.integration.rate.rate
import org.stellar.reference.integration.sep24.sep24
import org.stellar.reference.integration.sep24.testSep24
import org.stellar.reference.integration.uniqueaddress.UniqueAddressService
import org.stellar.reference.integration.uniqueaddress.uniqueAddress
import org.stellar.reference.plugins.*
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
        configureAuth(cfg)
        configureRouting(cfg)
        install(CORS) {
          anyHost()
          allowHeader(HttpHeaders.Authorization)
          allowHeader(HttpHeaders.ContentType)
        }
        install(RequestLoggerPlugin)
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

fun Application.configureAuth(cfg: Config) {
  when (cfg.integrationAuth.authType) {
    IntegrationAuth.Type.JWT ->
      authentication {
        jwt("integration-auth") {
          verifier(
            JWT.require(Algorithm.HMAC256(cfg.integrationAuth.platformToAnchorSecret)).build()
          )
          challenge { _, _ ->
            call.respond(HttpStatusCode.Unauthorized, "Token is invalid or expired")
          }
        }
      }
    IntegrationAuth.Type.API_KEY -> {
      TODO("API key auth not implemented yet")
    }
    IntegrationAuth.Type.NONE -> {
      log.warn("Authentication is disabled. Endpoints are not secured.")
      authentication { basic("integration-auth") { skipWhen { true } } }
    }
  }
}

fun Application.configureRouting(cfg: Config) {
  routing {
    // TODO: move the DI somewhere else
    val helper = Sep24Helper(cfg)
    val depositService = DepositService(cfg)
    val withdrawalService = WithdrawalService(cfg)
    val eventService = EventService()
    val database =
      Database.connect(
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
      )
    val customerRepo = JdbcCustomerRepository(database)
    val quotesRepo = JdbcQuoteRepository(database)
    customerRepo.init()
    quotesRepo.init()
    val customerService = CustomerService(customerRepo)
    val feeService = FeeService(customerRepo)
    val rateService = RateService(quotesRepo)
    val uniqueAddressService = UniqueAddressService(cfg.appSettings)

    sep24(helper, depositService, withdrawalService, cfg.sep24.interactiveJwtKey)
    event(eventService)
    customer(customerService)
    fee(feeService)
    rate(rateService)
    uniqueAddress(uniqueAddressService)

    if (cfg.sep24.enableTest) {
      testSep24(helper, depositService, withdrawalService, cfg.sep24.interactiveJwtKey)
    }
  }
}
