package org.stellar.reference.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.stellar.reference.callbacks.customer.CustomerService
import org.stellar.reference.callbacks.customer.customer
import org.stellar.reference.callbacks.fee.FeeService
import org.stellar.reference.callbacks.fee.fee
import org.stellar.reference.callbacks.interactive.sep24Interactive
import org.stellar.reference.callbacks.rate.RateService
import org.stellar.reference.callbacks.rate.rate
import org.stellar.reference.callbacks.test.testCustomer
import org.stellar.reference.callbacks.uniqueaddress.UniqueAddressService
import org.stellar.reference.callbacks.uniqueaddress.uniqueAddress
import org.stellar.reference.dao.JdbcCustomerRepository
import org.stellar.reference.dao.JdbcQuoteRepository
import org.stellar.reference.data.Config
import org.stellar.reference.event.EventService
import org.stellar.reference.event.event
import org.stellar.reference.sep24.DepositService
import org.stellar.reference.sep24.WithdrawalService
import org.stellar.reference.sep24.sep24
import org.stellar.reference.sep24.testSep24
import org.stellar.reference.service.SepHelper
import org.stellar.reference.service.sep31.ReceiveService

fun Application.configureRouting(cfg: Config) = routing {
  val helper = SepHelper(cfg)
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
  val receiveService = ReceiveService(cfg)

  sep24(helper, depositService, withdrawalService, cfg.sep24.interactiveJwtKey)
  event(eventService)
  customer(customerService)
  fee(feeService)
  rate(rateService)
  uniqueAddress(uniqueAddressService)
  sep24Interactive()

  if (cfg.appSettings.enableTest) {
    testSep24(helper, depositService, withdrawalService, cfg.sep24.interactiveJwtKey)
    testSep31(receiveService)
  }
  if (cfg.appSettings.isTest) {
    testCustomer(customerService)
  }
}
