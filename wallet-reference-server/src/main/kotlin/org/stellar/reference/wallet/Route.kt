package org.stellar.reference.wallet

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Duration
import java.time.Instant
import java.util.*
import org.stellar.anchor.api.sep.sep24.Sep24GetTransactionResponse
import org.stellar.anchor.platform.event.ClientStatusCallbackHandler
import org.stellar.anchor.util.GsonUtils
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

      if (!verifySignature(header, body, config.anchor.domain, signer)) {
        call.response.status(HttpStatusCode.Forbidden)
        //        return@post
      }

      val event = gson.fromJson(body, Sep24GetTransactionResponse::class.java)
      callbackEventService.processCallback(event)
      call.respond("POST /callback received")
    }
    get { call.respond(gson.toJson(callbackEventService.getCallbacks(call.parameters["txnId"]))) }
  }

  route("/callbacks/latest") { get { call.respond("GET /callbacks/latest") } }
}

fun verifySignature(header: String?, body: String?, domain: String?, signer: KeyPair?): Boolean {
  if (header == null) {
    return false
  }
  val tokens = header.split(",")
  if (tokens.size != 2) {
    return false
  }
  // t=timestamp
  val timestampTokens = tokens[0].trim().split("=")
  if (timestampTokens.size != 2 || timestampTokens[0] != "t") {
    return false
  }
  val timestampLong = timestampTokens[1].trim().toLongOrNull() ?: return false
  val timestamp = Instant.ofEpochSecond(timestampLong)

  if (Duration.between(timestamp, Instant.now()).toMinutes() > 2) {
    // timestamp is older than 2 minutes
    return false
  }

  // s=signature
  val sigTokens = tokens[1].trim().split("=", limit = 2)
  if (sigTokens.size != 2 || sigTokens[0] != "s") {
    return false
  }

  val sigBase64 = sigTokens[1].trim()
  if (sigBase64.isEmpty()) {
    return false
  }

  val signature = Base64.getDecoder().decode(sigBase64)

  if (body == null) {
    return false
  }

  val payloadToVerify = "${timestampLong}.${domain}.${body}"
  if (signer == null) {
    return false
  }

  if (!signer.verify(payloadToVerify.toByteArray(), signature)) {
    return false
  }

  return true
}

val signerSecret =
  KeyPair.fromSecretSeed("SAX3AH622R2XT6DXWWSRIDCMMUCCMATBZ5U6XKJWDO7M2EJUBFC3AW5X")
val signerPublic = KeyPair.fromAccountId("GCHLHDBOKG2JWMJQBTLSL5XG6NO7ESXI2TAQKZXCXWXB5WI2X6W233PR")

fun main() {
  val request =
    ClientStatusCallbackHandler.buildRequestBody(
      signerSecret,
      "test_payload",
      "localhost:8080",
      "http://localhost:8092/callbacks"
    )
  val signature = request.header("Signature")

  val result = verifySignature(signature, "test_payload", "localhost:8080", signerPublic)
  println(result)
}
