package org.stellar.reference.event

import com.google.gson.Gson
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.http.HttpStatus
import org.stellar.anchor.api.callback.SendEventResponse
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.data.SendEventRequest

fun Route.event(eventService: EventService) {
  val gson: Gson = GsonUtils.getInstance()

  route("/event") {
    // The `POST /event` endpoint of the Callback API to receive an event.
    post {
      val receivedEventJson = call.receive<String>()
      val receivedEvent = gson.fromJson(receivedEventJson, SendEventRequest::class.java)
      eventService.processEvent(receivedEvent)
      call.respond(gson.toJson(SendEventResponse(HttpStatus.SC_OK, "event processed")))
    }
  }
}
