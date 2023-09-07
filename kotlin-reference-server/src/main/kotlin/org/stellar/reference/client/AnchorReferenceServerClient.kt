package org.stellar.reference.client

import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.stellar.anchor.api.callback.SendEventRequest
import org.stellar.anchor.api.callback.SendEventResponse
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.util.GsonUtils

class AnchorReferenceServerClient(val endpoint: Url) {
  val gson = GsonUtils.getInstance()
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
  suspend fun getEvents(txnId: String? = null): List<AnchorEvent> {
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

    // Parse the JSON string into a list of Person objects
    return gson.fromJson(response.body<String>(), object : TypeToken<List<AnchorEvent>>() {}.type)
  }

  suspend fun getLatestEvent(): AnchorEvent? {
    val response =
      client.get {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/events/latest"
        }
      }
    return gson.fromJson(response.body<String>(), AnchorEvent::class.java)
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
