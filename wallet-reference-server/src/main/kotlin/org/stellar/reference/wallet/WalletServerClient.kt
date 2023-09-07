package org.stellar.reference.wallet

import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.stellar.anchor.api.callback.SendEventRequest
import org.stellar.anchor.api.callback.SendEventResponse
import org.stellar.anchor.api.sep.sep24.Sep24GetTransactionResponse
import org.stellar.anchor.util.GsonUtils

class WalletServerClient(val endpoint: Url = Url("http://localhost:8092")) {
  val gson = GsonUtils.getInstance()
  val client = HttpClient()

  suspend fun sendCallback(sendEventRequest: SendEventRequest): SendEventResponse {
    val response =
      client.post {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/callback"
        }
        contentType(ContentType.Application.Json)
        setBody(gson.toJson(sendEventRequest))
      }

    return gson.fromJson(response.body<String>(), SendEventResponse::class.java)
  }

  suspend fun <T> getCallbackHistory(txnId: String? = null, responseType: Class<T>): List<T> {
    val response =
      client.get {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/callbacks"
          txnId?.let { parameter("txnId", it) }
        }
      }

    // Parse the JSON string into a list of Person objects
    return gson.fromJson(
      response.body<String>(),
      TypeToken.getParameterized(List::class.java, responseType).type
    )
  }

  suspend fun getLatestCallback(): Sep24GetTransactionResponse? {
    val response =
      client.get {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/callbacks/latest"
        }
      }
    return gson.fromJson(response.body<String>(), Sep24GetTransactionResponse::class.java)
  }

  suspend fun clearCallbacks() {
    client.delete {
      url {
        this.protocol = endpoint.protocol
        host = endpoint.host
        port = endpoint.port
        encodedPath = "/events"
      }
    }
  }
}
