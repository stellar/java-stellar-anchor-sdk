package org.stellar.reference.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.stellar.anchor.api.platform.GetTransactionResponse
import org.stellar.anchor.api.platform.PatchTransactionsRequest
import org.stellar.anchor.api.platform.PatchTransactionsResponse
import org.stellar.anchor.util.GsonUtils

class PlatformClient(private val httpClient: HttpClient, private val endpoint: String) {
  suspend fun getTransaction(id: String): GetTransactionResponse {
    val response = httpClient.request("$endpoint/transactions/$id") { method = HttpMethod.Get }
    if (response.status != HttpStatusCode.OK) {
      throw Exception("Error getting transaction: ${response.status}")
    }
    // TODO: use ContentNegotiation plugin
    return GsonUtils.getInstance()
      .fromJson(response.body<String>(), GetTransactionResponse::class.java)
  }

  suspend fun patchTransactions(request: PatchTransactionsRequest): PatchTransactionsResponse {
    val response =
      httpClient.request("$endpoint/transactions") {
        method = HttpMethod.Patch
        setBody(GsonUtils.getInstance().toJson(request))
        contentType(ContentType.Application.Json)
      }
    if (response.status != HttpStatusCode.OK) {
      throw Exception("Error patching transaction: ${response.status}")
    }
    return GsonUtils.getInstance()
      .fromJson(response.body<String>(), PatchTransactionsResponse::class.java)
  }
}
