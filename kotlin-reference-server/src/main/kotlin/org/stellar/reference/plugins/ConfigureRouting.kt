package org.stellar.reference.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.stellar.reference.dao.JdbcCustomerRepository
import org.stellar.reference.dao.JdbcQuoteRepository
import org.stellar.reference.data.Config
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
import org.stellar.reference.sep24.DepositService
import org.stellar.reference.sep24.Sep24Helper
import org.stellar.reference.sep24.WithdrawalService

fun Application.configureRouting(cfg: Config) = routing {
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
