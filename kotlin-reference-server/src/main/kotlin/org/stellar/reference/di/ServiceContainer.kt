package org.stellar.reference.di

import io.ktor.client.*
import io.ktor.client.plugins.*
import org.jetbrains.exposed.sql.Database
import org.stellar.reference.callbacks.customer.CustomerService
import org.stellar.reference.callbacks.fee.FeeService
import org.stellar.reference.callbacks.rate.RateService
import org.stellar.reference.callbacks.uniqueaddress.UniqueAddressService
import org.stellar.reference.client.PlatformClient
import org.stellar.reference.dao.JdbcCustomerRepository
import org.stellar.reference.dao.JdbcQuoteRepository
import org.stellar.reference.event.EventService
import org.stellar.reference.sep24.DepositService
import org.stellar.reference.sep24.Sep24Helper
import org.stellar.reference.sep24.WithdrawalService
import org.stellar.sdk.Server

object ServiceContainer {
  val eventService = EventService()
  val helper = Sep24Helper(ConfigContainer.getInstance().config)
  val depositService = DepositService(ConfigContainer.getInstance().config)
  val withdrawalService = WithdrawalService(ConfigContainer.getInstance().config)

  private val database =
    Database.connect(
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      driver = "org.h2.Driver",
      user = "sa",
      password = ""
    )
  private val customerRepo = JdbcCustomerRepository(database)
  private val quotesRepo = JdbcQuoteRepository(database)
  val customerService = CustomerService(customerRepo)
  val feeService = FeeService(customerRepo)
  val rateService = RateService(quotesRepo)
  val uniqueAddressService = UniqueAddressService(ConfigContainer.getInstance().config.appSettings)
  val horizon = Server(ConfigContainer.getInstance().config.appSettings.horizonEndpoint)
  val platform =
    PlatformClient(
      HttpClient {
        install(HttpTimeout) {
          requestTimeoutMillis = 300000
          connectTimeoutMillis = 300000
          socketTimeoutMillis = 300000
        }
      },
      ConfigContainer.getInstance().config.appSettings.platformApiEndpoint
    )
}
