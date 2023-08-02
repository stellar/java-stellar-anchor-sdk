package org.stellar.reference.wallet.plugins

import com.amazonaws.util.ValidationUtils.assertNotNull
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import java.util.concurrent.TimeUnit
import org.stellar.anchor.sep10.Sep10Helper
import org.stellar.anchor.util.NetUtil
import org.stellar.reference.wallet.callback.CallbackEventService
import org.stellar.sdk.KeyPair

fun Route.callback(CallbackEventService: CallbackEventService) {
  route("/callback") {
    // The `POST /callback` endpoint of the CallbackAPI to receive an event.
    post {

      // Extract TS from request header and verify the ts difference is within 1-2 minutes
      val header = call.request.headers["Signature"]
      val tsFromRequest = header?.split(", ")?.get(0)?.substring(2)
      assertNotNull(tsFromRequest, "Timestamp should not be null")
      val currentTs = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toString()
      if (currentTs.toLong() - tsFromRequest!!.toLong() > 120) {
        throw Exception()
      }

      // Extract and decode the signature
      val signatureFromRequest = Base64.getDecoder().decode(header.split(", ")[1].substring(2))
      // Prepare the payload to verify
      val body = call.receive<String>()
      val payloadToVerify = tsFromRequest + "." + call.request.host() + "." + body

      val domain = NetUtil.getDomainFromURL(call.request.host())
      val clientSigningKey = Sep10Helper.fetchSigningKeyFromClientDomain(domain)
      val signer = KeyPair.fromSecretSeed(clientSigningKey)
      val signatureToVerify = signer.sign(payloadToVerify.toByteArray())

      //      val signatureToVerify = signer.sign(payloadToVerify.toByteArray())

      call.respond("POST /callback")
    }
    get { call.respond("GET /callback ") }
  }
}
