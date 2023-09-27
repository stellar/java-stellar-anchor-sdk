package org.stellar.reference.wallet

import com.google.gson.JsonObject
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.wallet.CallbackService.Companion.verifySignature
import org.stellar.sdk.KeyPair

var signer: KeyPair? = null
val gson = GsonUtils.getInstance()

fun Route.callback(config: Config, callbackEventService: CallbackService) {
  route("/callbacks") {
    // The `POST /callback` endpoint of the CallbackAPI to receive an event.
    post {
      // Extract TS from request header and verify the ts difference is within 1-2 minutes
      val header = call.request.headers["Signature"]
      val body = call.receive<String>()
      if (signer == null) {
        signer = KeyPair.fromAccountId(fetchSigningKey(config))
      }

      if (!verifySignature(header, body, config.wallet.hostname, signer)) {
        call.response.status(HttpStatusCode.Forbidden)
        return@post
      }

      val event: JsonObject = gson.fromJson(body, JsonObject::class.java)
      callbackEventService.processCallback(event)
      call.respond("POST /callback received")
    }
    get { call.respond(gson.toJson(callbackEventService.getCallbacks(call.parameters["txnId"]))) }
  }

  route("/callbacks/latest") { get { call.respond("GET /callbacks/latest") } }
}
