package org.stellar.anchor.platform

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.stellar.platform.apis.platform.requests.PatchTransactionsRequest
import org.stellar.platform.apis.platform.responses.GetTransactionResponse
import org.stellar.platform.apis.platform.responses.PatchTransactionsResponse

class PlatformApiClient(private val endpoint: String) : SepClient() {
  fun getTransaction(id: String): GetTransactionResponse {
    val request =
      Request.Builder()
        .url("$endpoint/transactions/$id")
        .header("Content-Type", "application/json")
        .get()
        .build()
    val responseBody = handleResponse(client.newCall(request).execute())
    return gson.fromJson(responseBody, GetTransactionResponse::class.java)
  }

  fun patchTransaction(txnRequest: PatchTransactionsRequest): PatchTransactionsResponse? {
    val urlBuilder = this.endpoint.toHttpUrl().newBuilder().addPathSegment("transactions")
    val requestBody = gson.toJson(txnRequest).toRequestBody(TYPE_JSON)
    val request =
      Request.Builder()
        .url(urlBuilder.build())
        .header("Content-Type", "application/json")
        .post(requestBody)
        .build()
    var response = client.newCall(request).execute()
    return gson.fromJson(handleResponse(response), PatchTransactionsResponse::class.java)
  }
}
