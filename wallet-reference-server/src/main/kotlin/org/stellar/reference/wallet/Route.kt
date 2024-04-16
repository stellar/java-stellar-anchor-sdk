package org.stellar.reference.wallet

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.jsonwebtoken.Jwts
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant
import kotlinx.datetime.Clock
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.wallet.CallbackService.Companion.verifySignature
import org.stellar.sdk.KeyPair
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.auth.DefaultAuthHeaderSigner
import org.stellar.walletsdk.auth.DomainAuthHeaderSigner
import org.stellar.walletsdk.auth.WalletSigner
import org.stellar.walletsdk.horizon.AccountKeyPair
import org.stellar.walletsdk.horizon.SigningKeyPair
import org.stellar.walletsdk.horizon.sign
import org.stellar.walletsdk.util.toJava

var signer: KeyPair? = null
val gson: Gson = GsonUtils.getInstance()

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

fun Route.noncustodial(config: Config) {
  val wallet = Wallet(StellarConfiguration.Testnet)
  val key = SigningKeyPair.fromSecret(config.secret.key)
  val domainSigner =
    object : DefaultAuthHeaderSigner() {
      override suspend fun createToken(
        claims: Map<String, String>,
        clientDomain: String?,
        issuer: AccountKeyPair?
      ): String {
        val timeExp = Instant.ofEpochSecond(Clock.System.now().plus(expiration).epochSeconds)
        val builder = createBuilder(timeExp, claims)

        builder.signWith(key.toJava().private, Jwts.SIG.EdDSA)

        return builder.compact()
      }
    }

  route("/signChallenge") {
    post {
      val body = call.receive<WalletSigner.DomainSigner.SigningData>()
      val transaction = wallet.stellar().decodeTransaction(body.transaction)
      val signed = transaction.sign(key).toEnvelopeXdrBase64()
      call.respond(WalletSigner.DomainSigner.SigningData(signed, body.networkPassphrase))
    }
  }

  route("/signHeader") {
    post {
      val body = call.receive<DomainAuthHeaderSigner.JWTSignData>()
      val token = domainSigner.createToken(body.claims, body.clientDomain, null)
      call.respond(DomainAuthHeaderSigner.SignedJWT(token))
    }
  }
}
