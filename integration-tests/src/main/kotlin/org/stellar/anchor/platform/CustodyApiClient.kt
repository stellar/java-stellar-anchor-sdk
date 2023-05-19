package org.stellar.anchor.platform

import com.google.gson.reflect.TypeToken
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest
import org.stellar.anchor.api.custody.GenerateDepositAddressResponse

class CustodyApiClient(private val endpoint: String, private val jwt: String) : SepClient() {

  fun generateDepositAddress(asset: String): GenerateDepositAddressResponse {
    val url = "$endpoint/transactions/payments/assets/$asset/address"
    val responseBody = httpPost(url, mapOf<String, String>(), jwt)
    return gson.fromJson(responseBody, GenerateDepositAddressResponse::class.java)
  }

  fun createTransaction(custodyTransaction: CreateCustodyTransactionRequest) {
    val url = "$endpoint/transactions"
    val type = object : TypeToken<Map<String?, *>?>() {}.type
    val requestBody: Map<String, Any> = gson.fromJson(gson.toJson(custodyTransaction), type)
    httpPost(url, requestBody, jwt)
  }

  fun createTransactionPayment(transactionId: String) {
    val url = "$endpoint/transactions/$transactionId/payments"
    httpPost(url, mapOf<String, String>(), jwt)
  }
}
