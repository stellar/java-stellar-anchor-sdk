package org.stellar.reference.client

import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.stellar.anchor.api.callback.SendEventRequest
import org.stellar.anchor.api.callback.SendEventResponse
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.data.SendEventRequestPayload

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
  suspend fun getEvents(txnId: String? = null): List<SendEventRequestPayload> {
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
    return gson.fromJson(
      response.body<String>(),
      object : TypeToken<List<SendEventRequestPayload>>() {}.type
    )
  }

  suspend fun getLatestEvent(): SendEventRequestPayload? {
    val response =
      client.get {
        url {
          this.protocol = endpoint.protocol
          host = endpoint.host
          port = endpoint.port
          encodedPath = "/events/latest"
        }
      }
    return gson.fromJson(response.body<String>(), SendEventRequestPayload::class.java)
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
}
