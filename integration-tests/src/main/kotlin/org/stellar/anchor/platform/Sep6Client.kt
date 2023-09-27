package org.stellar.anchor.platform

import org.stellar.anchor.api.sep.sep6.GetTransactionResponse
import org.stellar.anchor.api.sep.sep6.InfoResponse
import org.stellar.anchor.api.sep.sep6.StartDepositResponse
import org.stellar.anchor.api.sep.sep6.StartWithdrawResponse
import org.stellar.anchor.util.Log

class Sep6Client(private val endpoint: String, private val jwt: String) : SepClient() {
  fun getInfo(): InfoResponse {
    Log.info("SEP6 $endpoint/info")
    val responseBody = httpGet("$endpoint/info")
    return gson.fromJson(responseBody, InfoResponse::class.java)
  }

  fun deposit(request: Map<String, String>): StartDepositResponse {
    val baseUrl = "$endpoint/deposit?"
    val url = request.entries.fold(baseUrl) { acc, entry -> "$acc${entry.key}=${entry.value}&" }

    Log.info("SEP6 $url")
    val responseBody = httpGet(url, jwt)
    return gson.fromJson(responseBody, StartDepositResponse::class.java)
  }

  fun withdraw(request: Map<String, String>): StartWithdrawResponse {
    val baseUrl = "$endpoint/withdraw?"
    val url = request.entries.fold(baseUrl) { acc, entry -> "$acc${entry.key}=${entry.value}&" }

    val responseBody = httpGet(url, jwt)
    return gson.fromJson(responseBody, StartWithdrawResponse::class.java)
  }

  fun getTransaction(request: Map<String, String>): GetTransactionResponse {
    val baseUrl = "$endpoint/transaction?"
    val url = request.entries.fold(baseUrl) { acc, entry -> "$acc${entry.key}=${entry.value}&" }

    Log.info("SEP6 $url")
    val responseBody = httpGet(url, jwt)
    return gson.fromJson(responseBody, GetTransactionResponse::class.java)
  }
}
