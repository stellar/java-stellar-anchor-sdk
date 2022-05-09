package org.stellar.anchor.platform

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.stellar.anchor.api.sep.sep31.Sep31InfoResponse
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionResponse

class Sep31Client(private val endpoint: String, private val jwt: String) : SepClient() {
  fun getInfo(): Sep31InfoResponse {
    println("$endpoint/info")
    val responseBody = httpGet("$endpoint/info", jwt)
    return gson.fromJson(responseBody, Sep31InfoResponse::class.java)
  }

  fun postTransaction(txnRequest: Sep31PostTransactionRequest): Sep31PostTransactionResponse {
    val urlBuilder = this.endpoint.toHttpUrl().newBuilder().addPathSegment("transactions")
    val requestBody = gson.toJson(txnRequest).toRequestBody(TYPE_JSON)
    val request =
      Request.Builder()
        .url(urlBuilder.build())
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .post(requestBody)
        .build()

    print(gson.toJson(txnRequest))
    var response = client.newCall(request).execute()
    return gson.fromJson(handleResponse(response), Sep31PostTransactionResponse::class.java)
  }
}
