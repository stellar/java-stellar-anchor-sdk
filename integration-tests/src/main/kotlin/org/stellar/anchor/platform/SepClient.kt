package org.stellar.anchor.platform

import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.springframework.http.HttpStatus
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.util.GsonUtils

open class SepClient {
  companion object {
    val gson: Gson = GsonUtils.getInstance()
    val client =
      OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()
  }
  fun httpGetWithJwt(url: String, jwt: String): String? {
    val builder =
      Request.Builder()
        .url("$url")
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer $jwt")
        .get()

    val request = builder.build()
    val response = client.newCall(request).execute()
    return handleResponse(response)
  }
  fun httpPostWithJwt(request: Any, jwt: String) {}

  fun handleResponse(response: Response): String? {
    val responseBody = response.body?.string()

    println("statusCode: " + response.code)
    println("responseBody: $responseBody")
    if (response.code == HttpStatus.FORBIDDEN.value()) {
      throw SepNotAuthorizedException("Forbidden")
    } else if (!listOf(
          HttpStatus.OK.value(),
          HttpStatus.CREATED.value(),
          HttpStatus.ACCEPTED.value()
        )
        .contains(response.code)
    ) {
      throw SepException(responseBody)
    }

    return responseBody
  }
}
