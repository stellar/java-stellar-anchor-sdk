package org.stellar.reference.event

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.core.readBytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.hc.core5.http.HttpStatus
import org.stellar.anchor.api.callback.SendEventResponse
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.data.SendEventRequest
import org.stellar.reference.di.AUTH_CONFIG_ENDPOINT

const val MAX_EVENT_BODY_BYTES = 1_048_576

// `EventService.channel` is a rendezvous channel, so every handler waits in `channel.send` holding
// its body and parsed event until the consumer takes it. The Platform sends one event at a time and
// retries a non-2xx with backoff, so a handful of concurrent handlers is plenty.
const val MAX_IN_FLIGHT_EVENTS = 16

// A permit is held while the body is read, so a client that trickles its body must not keep it
// forever. The Platform sends the whole body at once. Only the body read is timed out, never the
// wait for the consumer (`channel.send`), whose speed is the server's own.
val EVENT_BODY_READ_TIMEOUT = 10.seconds

fun Route.event(
  eventService: EventService,
  enableTestEndpoints: Boolean,
  bodyReadTimeout: Duration = EVENT_BODY_READ_TIMEOUT,
) {
  val gson: Gson = GsonUtils.getInstance()
  val inFlight = Semaphore(MAX_IN_FLIGHT_EVENTS)

  authenticate(AUTH_CONFIG_ENDPOINT) {
    route("/event") {
      // The `POST /event` endpoint of the Callback API to receive an event.
      post {
        // Refuse before reading the body: a refused request must not cost memory.
        if (!inFlight.tryAcquire()) {
          call.response.header(HttpHeaders.RetryAfter, "1")
          call.respond(HttpStatusCode.ServiceUnavailable, "Too many events in flight")
          return@post
        }
        try {
          // Read at most MAX_EVENT_BODY_BYTES + 1 bytes: enough to tell that the body is too
          // large, without buffering an attacker-sized body (a chunked body has no Content-Length).
          val bodyBytes =
            withTimeoutOrNull(bodyReadTimeout) {
              call.receiveChannel().readRemaining(MAX_EVENT_BODY_BYTES + 1L).readBytes()
            }
          if (bodyBytes == null) {
            call.respond(HttpStatusCode.RequestTimeout, "Timed out reading the event body")
            return@post
          }
          if (bodyBytes.size > MAX_EVENT_BODY_BYTES) {
            call.respond(
              HttpStatusCode.PayloadTooLarge,
              "Event body exceeds $MAX_EVENT_BODY_BYTES bytes"
            )
            return@post
          }
          val receivedEventJson = String(bodyBytes, Charsets.UTF_8)
          val receivedEvent = gson.fromJson(receivedEventJson, SendEventRequest::class.java)
          eventService.processEvent(receivedEvent, bodyBytes.size)
          call.respond(gson.toJson(SendEventResponse(HttpStatus.SC_OK, "event processed")))
        } finally {
          inFlight.release()
        }
      }
    }

    if (enableTestEndpoints) {
      route("/events") {
        // Test endpoint to get the events recorded by the reference server.
        // The `txnId` parameter is optional. If it is provided, only the events with the given
        // `txnId`
        // will be returned.
        get { call.respond(gson.toJson(eventService.getEvents(call.parameters["txnId"]))) }
        // Test endpoint to clear the events recorded by the reference server.
        delete {
          eventService.clearEvents()
          call.respond("Events cleared")
        }
      }
      route("/events/latest") {
        // Test endpoint to get the latest event recorded by the reference server.
        get {
          val latestEvent = eventService.getLatestEvent()
          if (latestEvent != null) {
            call.respond(gson.toJson(latestEvent))
          } else {
            call.respond(HttpStatusCode.NotFound)
          }
        }
      }
    }
  }
}
