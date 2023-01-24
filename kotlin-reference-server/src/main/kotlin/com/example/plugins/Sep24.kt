package com.example.plugins

import com.example.ClientException
import com.example.jwt.JwtDecoder
import com.example.sep24.DepositService
import com.example.sep24.Sep24Helper
import com.example.sep24.WithdrawalService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging

val log = KotlinLogging.logger {}

fun Route.sep24(
  sep24: Sep24Helper,
  depositService: DepositService,
  withdrawalService: WithdrawalService
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
      val amountIn = transaction.amountIn.amount.toBigDecimal()

      try {
        when (transaction.kind.lowercase()) {
          "deposit" -> {
            depositService.getClientInfo(transactionId)

            val account = transaction.toAccount ?: throw ClientException("Missing toAccount field")
            val assetCode =
              transaction.requestAssetCode
                ?: throw ClientException("Missing requestAssetCode field")
            val assetIssuer =
              transaction.requestAssetIssuer
                ?: throw ClientException("Missing requestAssetIssuer field")
            val memo = transaction.memo
            val memoType = transaction.memoType

            call.respondText("The sep24 interactive deposit has been successfully started.")

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
            withdrawalService.getClientInfo(transactionId)

            call.respondText("The sep24 interactive withdrawal has been successfully started.")

            // Run deposit processing asynchronously
            CoroutineScope(Job()).launch {
              withdrawalService.processWithdrawal(transactionId, amountIn)
            }
          }
          else ->
            call.respondText(
              "The only supported operations are \"deposit\" or \"withdrawal\"",
              status = HttpStatusCode.BadRequest
            )
        }
      } catch (e: ClientException) {
        call.respondText(e.message!!, status = HttpStatusCode.BadRequest)
      } catch (e: Exception) {
        call.respondText(
          "Error occurred: ${e.message}",
          status = HttpStatusCode.InternalServerError
        )
      }
    }
  }
}
