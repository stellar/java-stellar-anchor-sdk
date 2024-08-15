package org.stellar.reference.wallet

import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse
import org.stellar.anchor.util.GsonUtils

class WalletServerClient(val endpoint: Url = Url("http://localhost:8092")) {
  val gson = GsonUtils.getInstance()
  val client = HttpClient()

  suspend fun <T> getTransactionCallbacks(
    sep: String,
    txnId: String? = null,
    responseType: Class<T>
  ): List<T> {
    val response =
      client.get {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/callbacks/$sep"
          txnId?.let { parameter("txnId", it) }
        }
      }

    return gson.fromJson(
      response.body<String>(),
      TypeToken.getParameterized(List::class.java, responseType).type
    )
  }

  suspend fun <T> pollTransactionCallbacks(
    sep: String,
    txnId: String?,
    expected: Int,
    responseType: Class<T>
  ): List<T> {
    var retries = 5
    var callbacks: List<T> = listOf()
    while (retries > 0) {
      callbacks = getTransactionCallbacks(sep, txnId, responseType)
      if (callbacks.size >= expected) {
        return callbacks
      }
      delay(5.seconds)
      retries--
    }
    return callbacks
  }

  suspend fun getCustomerCallbacks(id: String?, expected: Int): List<Sep12GetCustomerResponse> {
    val response =
      client.get {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/callbacks/sep12"
          id?.let { parameter("id", it) }
        }
      }

    return gson.fromJson(
      response.body<String>(),
      TypeToken.getParameterized(List::class.java, Sep12GetCustomerResponse::class.java).type
    )
  }

  suspend fun pollCustomerCallbacks(id: String?, expected: Int): List<Sep12GetCustomerResponse> {
    var retries = 5
    var callbacks: List<Sep12GetCustomerResponse> = listOf()
    while (retries > 0) {
      callbacks = getCustomerCallbacks(id, expected)
      if (callbacks.size >= expected) {
        return callbacks
      }
      delay(5.seconds)
      retries--
    }
    return callbacks
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
