package org.stellar.reference.wallet

import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.stellar.anchor.api.callback.SendEventRequest
import org.stellar.anchor.api.callback.SendEventResponse
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

  suspend fun <T> getCallbacks(txnId: String? = null, responseType: Class<T>): List<T> {
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

    return gson.fromJson(
      response.body<String>(),
      TypeToken.getParameterized(List::class.java, responseType).type
    )
  }

  suspend fun <T> pollCallbacks(txnId: String?, expected: Int, responseType: Class<T>): List<T> {
    var retries = 5
    var callbacks: List<T> = listOf()
    while (retries > 0) {
      callbacks = getCallbacks(txnId, responseType)
      if (callbacks.size >= expected) {
        return callbacks
      }
      delay(5.seconds)
      retries--
    }
    return callbacks
  }

  suspend fun <T> getLatestCallback(): T? {
    val response =
      client.get {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/callbacks/latest"
        }
      }
    return gson.fromJson(response.body<String>(), object : TypeToken<T>() {}.type)
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
