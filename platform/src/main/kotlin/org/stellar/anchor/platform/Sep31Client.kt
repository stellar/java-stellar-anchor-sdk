package org.stellar.anchor.platform

import okhttp3.Request
import org.stellar.anchor.dto.sep31.Sep31InfoResponse

class Sep31Client(private val endpoint: String, private val jwt: String) : SepClient() {
  fun getInfo(): Sep31InfoResponse {
    println("$endpoint/info")
    val request =
      Request.Builder()
        .url("$endpoint/info")
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer $jwt")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val responseBody = handleResponse(response)
    return gson.fromJson(responseBody, Sep31InfoResponse::class.java)
  }
}
