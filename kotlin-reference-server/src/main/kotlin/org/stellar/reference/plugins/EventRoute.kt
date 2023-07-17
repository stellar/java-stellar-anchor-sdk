package org.stellar.reference.plugins

import com.google.gson.Gson
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.api.callback.SendEventResponse
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.data.SendEventRequest
import org.stellar.reference.event.EventService

fun Route.event(eventService: EventService) {
  val gson: Gson = GsonUtils.getInstance()

  route("/events") {
    post {
      val receivedEventJson = call.receive<String>()
      println(receivedEventJson)
      val receivedEvent = gson.fromJson(receivedEventJson, SendEventRequest::class.java)
      eventService.processEvent(receivedEvent)
      call.respond(gson.toJson(SendEventResponse("event processed")))
    }
    get { call.respond(eventService.getEvents()) }
    delete {
      eventService.clearEvents()
      call.respond("Events cleared")
    }
  }
  route("/events/latest") {
    get { call.respond(eventService.getLatestEvent() ?: "No events received yet") }
  }
}
