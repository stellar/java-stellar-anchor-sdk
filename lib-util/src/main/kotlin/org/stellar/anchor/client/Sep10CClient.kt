package org.stellar.anchor.client

import net.i2p.crypto.eddsa.Utils
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest
import org.stellar.anchor.api.sep.sep10c.ChallengeResponse
import org.stellar.anchor.api.sep.sep10c.ValidationRequest
import org.stellar.sdk.SorobanServer
import org.stellar.sdk.xdr.SorobanAuthorizationEntry

class Sep10CClient(
  private val endpoint: String,
  private val serverAccount: String,
  private val rpc: SorobanServer,
) : SepClient() {

  fun getChallenge(request: ChallengeRequest): ChallengeResponse {
    val params = mutableMapOf<String, String>()
    params["account"] = request.account ?: ""
    params["memo"] = request.memo ?: ""
    params["home_domain"] = request.homeDomain ?: ""
    params["client_domain"] = request.clientDomain ?: ""

    val queryString =
      params.filter { it.value.isNotEmpty() }.map { "${it.key}=${it.value}" }.joinToString("&")

    val url = "${this.endpoint}?$queryString"
    val response = httpGet(url)
    return gson.fromJson(response, ChallengeResponse::class.java)
  }

  fun sign(challengeResponse: ChallengeResponse): ValidationRequest {
    val serverSignature = Utils.hexToBytes(challengeResponse.serverSignature)
    val authorizedEntry =
      SorobanAuthorizationEntry.fromXdrBase64(challengeResponse.authorizationEntry)

    // Verify the server signed the authorized invocation
    val serverKeypair = rpc.getAccount(serverAccount).keyPair
    serverKeypair.verify(authorizedEntry.toXdrByteArray(), serverSignature)

    return ValidationRequest.builder()
      .authorizationEntry(challengeResponse.authorizationEntry)
      .serverSignature(challengeResponse.serverSignature)
      .build()
  }
}
