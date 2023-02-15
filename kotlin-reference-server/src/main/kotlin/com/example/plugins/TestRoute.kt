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

private val log = KotlinLogging.logger {}

fun Route.testSep24(
  sep24: Sep24Helper,
  depositService: DepositService,
  withdrawalService: WithdrawalService,
  jwtKey: String
) {
  route("/sep24/interactive") {
    get {
      log.info("Called /sep24/interactive with parameters ${call.parameters}")

      val token =
        JwtDecoder.decode(
          call.parameters["token"]
            ?: throw ClientException("Missing token parameter in the request"),
          jwtKey
        )

      val transactionId = token.transactionId

      if (token.expiration > System.currentTimeMillis()) {
        throw ClientException("Token expired")
      }

      val transaction = sep24.getTransaction(transactionId)
      val amountIn =
        transaction.amountExpected?.amount?.toBigDecimal()
          ?: throw ClientException("Missing amountExpected.amount field")

      try {
        when (transaction.kind.lowercase()) {
          "deposit" -> {
            val account =
              transaction.sourceAccount ?: throw ClientException("Missing source_account field")
            val asset =
              transaction.amountExpected.asset
                ?: throw ClientException("Missing amountExpected.asset field")
            val memo = transaction.memo
            val memoType = transaction.memoType

            if (!asset.startsWith("stellar:")) {
              throw ClientException("Invalid asset format")
            }

            call.respondText("The sep24 interactive deposit has been successfully started.")

            val stellarAsset = asset.replace("stellar:", "")

            // Run deposit processing asynchronously
            CoroutineScope(Job()).launch {
              depositService.processDeposit(
                transactionId,
                amountIn,
                account,
                stellarAsset,
                memo,
                memoType
              )
            }
          }
          "withdrawal" -> {
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
