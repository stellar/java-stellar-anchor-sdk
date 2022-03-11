package org.stellar.anchor.platform

import java.net.URL
import okhttp3.Request
import org.stellar.anchor.dto.sep10.ChallengeResponse
import org.stellar.anchor.dto.sep10.ValidationRequest
import org.stellar.anchor.dto.sep10.ValidationResponse
import org.stellar.anchor.util.OkHttpUtil
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Network
import org.stellar.sdk.Sep10Challenge

class Sep10Client(private val endpoint: String, private val account: String) : SepClient() {
  fun auth(signingSeed: String, serverAccount: String): String {
    val response = challenge()
    val txn = sign(response, signingSeed, serverAccount)
    return validate(ValidationRequest.of(txn))!!.token
  }
  fun challenge(): ChallengeResponse {
    val request =
        Request.Builder().url(String.format("%s?account=%s", this.endpoint, account)).get().build()

    val response = client.newCall(request).execute()
    val responseBody = response.body!!.string()
    return gson.fromJson(responseBody, ChallengeResponse::class.java)
  }

  fun sign(
      challengeResponse: ChallengeResponse,
      signingSeed: String,
      serverAccount: String
  ): String {
    val url = URL(endpoint)
    val domain = url.host
    val challengeTransaction =
        Sep10Challenge.readChallengeTransaction(
            challengeResponse.transaction,
            serverAccount,
            Network(challengeResponse.networkPassphrase),
            domain,
            domain)
    challengeTransaction.transaction.sign(KeyPair.fromSecretSeed(signingSeed))
    return challengeTransaction.transaction.toEnvelopeXdrBase64()
  }

  fun validate(request: ValidationRequest): ValidationResponse? {
    val request = OkHttpUtil.buildJsonPostRequest(this.endpoint, json(request))
    val response = client.newCall(request).execute()
    val responseBody = response.body!!.string()
    return gson.fromJson(responseBody, ValidationResponse::class.java)
  }
}
