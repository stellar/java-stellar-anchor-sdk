package com.example.sep24

import com.example.ClientException
import io.ktor.http.*
import mu.KotlinLogging

sealed interface Sep24ParametersProcessor {
  fun amount(parameters: Parameters): String?

  fun processUserInputWithdrawal(parameters: Parameters)
  fun processUserInputDeposit(parameters: Parameters)
}

object TestSep24ParametersProcessor : Sep24ParametersProcessor {
  override fun amount(parameters: Parameters): String? {
    return null
  }

  override fun processUserInputWithdrawal(parameters: Parameters) {
    // No KYC collected
  }

  override fun processUserInputDeposit(parameters: Parameters) {
    // No KYC collected
  }
}

object ProxySep24ParametersProcessor : Sep24ParametersProcessor {
  private val log = KotlinLogging.logger {}

  override fun amount(parameters: Parameters): String? {
    return parameters["amount"]
  }

  override fun processUserInputWithdrawal(parameters: Parameters) {
    val name = parameters["name"] ?: throw ClientException("Name was not specified")
    val surname = parameters["surname"] ?: throw ClientException("Surname was not specified")
    val email = parameters["email"] ?: throw ClientException("Email was not specified")

    val bankAccount =
      parameters["bank_account"] ?: throw ClientException("Bank account was not specified")
    val bank = parameters["bank"] ?: throw ClientException("Bank was not specified")

    log.info {
      "User requested withdrawal. Name = $name, surname = $surname, email = $email, bankAccount = $bankAccount, bank = $bank"
    }
  }

  override fun processUserInputDeposit(parameters: Parameters) {
    val name = parameters["name"] ?: throw ClientException("Name was not specified")
    val surname = parameters["surname"] ?: throw ClientException("Surname was not specified")
    val email = parameters["email"] ?: throw ClientException("Email was not specified")

    log.info { "USer requested deposit. Name = $name, surname = $surname, email = $email" }
  }
}
