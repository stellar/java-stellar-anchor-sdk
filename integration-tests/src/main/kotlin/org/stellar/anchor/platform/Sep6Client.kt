package org.stellar.anchor.platform

import org.stellar.anchor.api.sep.sep6.InfoResponse
import org.stellar.anchor.util.Log

class Sep6Client(private val endpoint: String) : SepClient() {
  fun getInfo(): InfoResponse {
    Log.info("SEP6 $endpoint/info")
    val responseBody = httpGet("$endpoint/info")
    return gson.fromJson(responseBody, InfoResponse::class.java)
  }
}
