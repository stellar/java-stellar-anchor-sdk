package org.stellar.anchor.client

import net.i2p.crypto.eddsa.Utils
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest
import org.stellar.anchor.api.sep.sep10c.ChallengeResponse
import org.stellar.anchor.api.sep.sep10c.ValidationRequest
import org.stellar.anchor.api.sep.sep10c.ValidationResponse
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.OkHttpUtil
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Network
import org.stellar.sdk.SorobanServer
import org.stellar.sdk.Util
import org.stellar.sdk.scval.Scv
import org.stellar.sdk.xdr.*

class Sep10CClient(
  private val endpoint: String,
  private val serverAccount: String,
  private val rpc: SorobanServer,
) : SepClient() {

  fun getChallenge(request: ChallengeRequest): ChallengeResponse {
    val params = mutableMapOf<String, String>()
    params["address"] = request.address ?: ""
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

    // Sign the authorized invocation
    val clientKeypair =
      KeyPair.fromSecretSeed("SCAWZ3DBU5UVT3SLDMLNPP4GUDP7WDCOYQDE5JHCTXHLS3TAJIZ4HJOC")

    val signer =
      object : Signer {
        override fun sign(preimage: HashIDPreimage): ByteArray {
          return clientKeypair.sign(Util.hash(preimage.toXdrByteArray()))
        }

        override fun publicKey(): ByteArray {
          return clientKeypair.publicKey
        }
      }

    val validUntilLedgerSeq = rpc.latestLedger.sequence + 100.toLong()
    val signedEntry =
      authorizeEntry(authorizedEntry, signer, validUntilLedgerSeq, Network(rpc.network.passphrase))

    return ValidationRequest.builder()
      .authorizationEntry(challengeResponse.authorizationEntry)
      .serverSignature(challengeResponse.serverSignature)
      .credentials(signedEntry.credentials.toXdrBase64())
      .build()
  }

  fun validate(validationRequest: ValidationRequest): ValidationResponse {
    val request =
      OkHttpUtil.buildJsonPostRequest(
        this.endpoint,
        GsonUtils.getInstance().toJson(validationRequest)
      )
    val response = client.newCall(request).execute()
    if (!response.isSuccessful) {
      throw RuntimeException("Failed to validate challenge: ${response.body!!.string()}")
    }
    val responseBody = response.body!!.string()
    return gson.fromJson(responseBody, ValidationResponse::class.java)
  }

  companion object {
    @JvmStatic
    fun authorizeEntry(
      entry: SorobanAuthorizationEntry,
      signer: Signer,
      validUntilLedgerSeq: Long,
      network: Network
    ): SorobanAuthorizationEntry {
      val clone = SorobanAuthorizationEntry.fromXdrByteArray(entry.toXdrByteArray())

      if (clone.credentials.discriminant != SorobanCredentialsType.SOROBAN_CREDENTIALS_ADDRESS) {
        return clone
      }

      val addressCredentials = clone.credentials.address
      addressCredentials.signatureExpirationLedger = Uint32(XdrUnsignedInteger(validUntilLedgerSeq))

      val preimage =
        HashIDPreimage.Builder()
          .discriminant(EnvelopeType.ENVELOPE_TYPE_SOROBAN_AUTHORIZATION)
          .sorobanAuthorization(
            HashIDPreimage.HashIDPreimageSorobanAuthorization.Builder()
              .networkID(Hash(network.networkId))
              .nonce(addressCredentials.nonce)
              .invocation(clone.rootInvocation)
              .signatureExpirationLedger(addressCredentials.signatureExpirationLedger)
              .build()
          )
          .build()

      val signature = signer.sign(preimage)
      val publicKey = signer.publicKey()

      val sigScVal =
        Scv.toMap(
          linkedMapOf(
            Scv.toSymbol("public_key") to Scv.toBytes(publicKey),
            Scv.toSymbol("signature") to Scv.toBytes(signature)
          )
        )
      addressCredentials.signature = Scv.toVec(listOf(sigScVal))

      return clone
    }

    interface Signer {
      fun sign(preimage: HashIDPreimage): ByteArray
      fun publicKey(): ByteArray
    }
  }
}
