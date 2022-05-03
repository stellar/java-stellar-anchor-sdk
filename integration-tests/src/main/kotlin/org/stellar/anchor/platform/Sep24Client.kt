package org.stellar.anchor.platform

import okhttp3.Request
import org.stellar.anchor.api.sep.sep24.InfoResponse
import org.stellar.anchor.api.sep.sep31.Sep31InfoResponse

class Sep24Client(private val endpoint: String, private val jwt: String) : SepClient() {

    fun getInfo(): InfoResponse {
        println("SEP24 $endpoint/info")
        val request =
            Request.Builder()
                .url("$endpoint/info")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $jwt")
                .get()
                .build()
        val response = client.newCall(request).execute()
        val responseBody = handleResponse(response)
        return gson.fromJson(responseBody, InfoResponse::class.java)
    }
}