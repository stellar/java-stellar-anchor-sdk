package org.stellar.reference.client

import com.google.gson.Gson
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

class AnchorReferenceServerClient(val endpoint: Url) {
  val gson: Gson = GsonUtils.getInstance()
  val client = HttpClient()

  suspend fun sendEvent(sendEventRequest: SendEventRequest): SendEventResponse {
    val response =
      client.post {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/event"
        }
        contentType(ContentType.Application.Json)
        setBody(gson.toJson(sendEventRequest))
      }

    return gson.fromJson(response.body<String>(), SendEventResponse::class.java)
  }

  suspend fun getEvents(txnId: String? = null): List<SendEventRequest> {
    val response =
      client.get {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/events"
          txnId?.let { parameter("txnId", it) }
        }
      }

    return gson.fromJson(
      response.body<String>(),
      object : TypeToken<List<SendEventRequest>>() {}.type
    )
  }

  suspend fun pollEvents(txnId: String? = null, expected: Int): List<SendEventRequest> {
    var retries = 5
    var events: List<SendEventRequest> = listOf()
    while (retries > 0) {
      events = getEvents(txnId)
      if (events.size >= expected) {
        return events
      }
      delay(5.seconds)
      retries--
    }
    return events
  }

  suspend fun getLatestEvent(): SendEventRequest? {
    val response =
      client.get {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/events/latest"
        }
      }
    return gson.fromJson(response.body<String>(), SendEventRequest::class.java)
  }

  suspend fun clearEvents() {
    client.delete {
      url {
        this.protocol = endpoint.protocol
        host = endpoint.host
        port = endpoint.port
        encodedPath = "/events"
      }
    }
  }

  /**
   * ATTENTION: this function is used for testing purposes only.
   *
   * <p>This endpoint is used to simulate SEP-31 flow
   */
  suspend fun processSep31Receive(transactionId: String) {
    client.post {
      url {
        this.protocol = endpoint.protocol
        host = endpoint.host
        port = endpoint.port
        encodedPath = "/sep31/transactions/$transactionId/process"
      }
    }
  }
}
