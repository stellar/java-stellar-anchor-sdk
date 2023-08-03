package org.stellar.reference.wallet.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlinx.coroutines.runBlocking
import org.stellar.anchor.api.sep.sep24.Sep24GetTransactionResponse
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.wallet.callback.CallbackEventService
import org.stellar.reference.wallet.data.Config
import org.stellar.reference.wallet.fetchSigningKey
import org.stellar.sdk.KeyPair

var signer: KeyPair? = null
val gson = GsonUtils.getInstance()

fun Route.callback(config: Config, callbackEventService: CallbackEventService) {
  route("/callbacks") {
    // The `POST /callback` endpoint of the CallbackAPI to receive an event.
    post {
      // Extract TS from request header and verify the ts difference is within 1-2 minutes
      val header = call.request.headers["Signature"]
      if (header == null) {
        call.response.status(HttpStatusCode.Forbidden)
        return@post
      }
      val tokens = header.split(",")
      if (tokens.size != 2) {
        call.response.status(HttpStatusCode.Forbidden)
        return@post
      }
      // t=timestamp
      val timestampTokens = tokens[0].trim().split("=")
      if (timestampTokens.size != 2 || timestampTokens[0] != "t") {
        call.response.status(HttpStatusCode.Forbidden)
        return@post
      }
      val timestampLong = timestampTokens[1].trim().toLongOrNull()
      if (timestampLong == null) {
        call.response.status(HttpStatusCode.Forbidden)
        return@post
      }
      val timestamp = Instant.ofEpochSecond(timestampLong)

      if (Duration.between(timestamp, Instant.now()).toMinutes() > 2) {
        // timestamp is older than 2 minutes
        call.response.status(HttpStatusCode.Forbidden)
        return@post
      }

      // s=signature
      val sigTokens = tokens[1].trim().split("=")
      if (sigTokens.size != 2 || sigTokens[0] != "s") {
        call.response.status(HttpStatusCode.Forbidden)
        return@post
      }

      val sigBase64 = sigTokens[1].trim()
      if (sigBase64.isEmpty()) {
        call.response.status(HttpStatusCode.Forbidden)
        return@post
      }

      val signature = Base64.getDecoder().decode(sigBase64)

      val body = call.receive<String>()
      if (body.isEmpty()) {
        call.response.status(HttpStatusCode.Forbidden)
        return@post
      }

      val payloadToVerify = "${timestamp}.${call.request.host()}.body"
      if (signer == null) {
        runBlocking { signer = KeyPair.fromSecretSeed(fetchSigningKey(config)) }
      }

      if (!signer!!.verify(payloadToVerify.toByteArray(), signature)) {
        call.response.status(HttpStatusCode.Forbidden)
        return@post
      }

      val event = gson.fromJson(body, Sep24GetTransactionResponse::class.java)
      callbackEventService.processCallback(event)

      //      val signatureToVerify = signer.sign(payloadToVerify.toByteArray())

      call.respond("POST /callback")
    }
    get { call.respond("GET /callbacks ") }
  }

  route("/callbacks/latest") { get { call.respond("GET /callbacks/latest") } }
}
