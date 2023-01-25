package com.example.plugins

import com.example.ClientException
import com.example.data.Success
import com.example.jwt.JwtDecoder
import com.example.sep24.DepositService
import com.example.sep24.Sep24Helper
import com.example.sep24.Sep24ParametersProcessor
import com.example.sep24.WithdrawalService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

fun Route.sep24(
  sep24: Sep24Helper,
  depositService: DepositService,
  withdrawalService: WithdrawalService,
  parametersProcessor: Sep24ParametersProcessor
) {
  route("/sep24/interactive") {
    get {
      log.info("Called /sep24/interactive with parameters ${call.parameters}")

      val token =
        JwtDecoder.decode(
          call.parameters["token"]
            ?: throw ClientException("Missing token parameter in the request")
        )

      val transactionId = token.jti

      if (token.exp > System.currentTimeMillis()) {
        throw ClientException("Token expired")
      }

      val transaction = sep24.getTransaction(transactionId)
      val amountIn =
        (transaction.amountIn?.amount ?: parametersProcessor.amount(call.parameters))
          ?.toBigDecimal()
          ?: throw ClientException("Amount was not specified")

      try {
        when (transaction.kind.lowercase()) {
          "deposit" -> {
            parametersProcessor.processUserInputDeposit(call.parameters)

            val account = transaction.toAccount ?: throw ClientException("Missing toAccount field")
            val assetCode =
              transaction.requestAssetCode
                ?: throw ClientException("Missing requestAssetCode field")
            val assetIssuer =
              transaction.requestAssetIssuer
                ?: throw ClientException("Missing requestAssetIssuer field")
            val memo = transaction.memo
            val memoType = transaction.memoType

            call.respond(Success(transactionId))

            // Run deposit processing asynchronously
            CoroutineScope(Job()).launch {
              depositService.processDeposit(
                transactionId,
                amountIn,
                account,
                assetCode,
                assetIssuer,
                memo,
                memoType
              )
            }
          }
          "withdrawal" -> {
            parametersProcessor.processUserInputWithdrawal(call.parameters)

            call.respond(Success(transactionId))

            // Run deposit processing asynchronously
            CoroutineScope(Job()).launch {
              withdrawalService.processWithdrawal(transactionId, amountIn)
            }
          }
          else ->
            call.respond(
              Error("The only supported operations are \"deposit\" or \"withdrawal\""),
            )
        }
      } catch (e: ClientException) {
        call.respond(Error(e.message!!))
      } catch (e: Exception) {
        call.respond(
          Error("Error occurred: ${e.message}"),
        )
      }
    }
  }
}
