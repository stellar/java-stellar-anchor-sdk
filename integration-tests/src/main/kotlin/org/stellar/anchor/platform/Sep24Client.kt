package org.stellar.anchor.platform

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.stellar.anchor.api.sep.sep24.GetTransactionResponse
import org.stellar.anchor.api.sep.sep24.InfoResponse
import org.stellar.anchor.api.sep.sep24.InteractiveTransactionResponse

class Sep24Client(private val endpoint: String, private val jwt: String) : SepClient() {

  fun getInfo(): InfoResponse {
    println("SEP24 $endpoint/info")
    val responseBody = httpGet("$endpoint/info", jwt)
    return gson.fromJson(responseBody, InfoResponse::class.java)
  }

  fun withdraw(requestData: Map<String, String>?): InteractiveTransactionResponse {
    println("SEP24 $endpoint/transactions/withdraw/interactive")
    val urlBuilder =
      this.endpoint.toHttpUrl().newBuilder().addPathSegments("transactions/withdraw/interactive")
    val requestBody = gson.toJson(requestData).toRequestBody(TYPE_JSON)
    val request =
      Request.Builder()
        .url(urlBuilder.build())
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .post(requestBody)
        .build()

    val response = client.newCall(request).execute()

    return gson.fromJson(handleResponse(response), InteractiveTransactionResponse::class.java)
  }

  fun getTransaction(id: String, assetCode: String): GetTransactionResponse {
    println("SEP24 $endpoint/transactions")
    val request =
      Request.Builder()
        .url("$endpoint/transaction?id=$id&asset_code=$assetCode")
        .header("Authorization", "Bearer $jwt")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val responseBody = handleResponse(response)
    return gson.fromJson(responseBody, GetTransactionResponse::class.java)
  }
}
