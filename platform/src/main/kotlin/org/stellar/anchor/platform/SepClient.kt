package org.stellar.anchor.platform

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.Instant
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Response
import org.springframework.http.HttpStatus
import org.stellar.anchor.exception.SepException
import org.stellar.anchor.exception.SepNotAuthorizedException
import org.stellar.anchor.util.InstantConverter

open class SepClient {
  companion object {
    val gson: Gson =
      GsonBuilder().registerTypeAdapter(Instant::class.java, InstantConverter()).create()
    val client =
      OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()
  }

  fun handleResponse(response: Response): String? {
    val responseBody = response.body?.string()

    println("statusCode: " + response.code)
    println("responseBody: $responseBody")
    if (response.code == HttpStatus.FORBIDDEN.value()) {
      throw SepNotAuthorizedException("Forbidden")
    } else if (!listOf(HttpStatus.OK.value(), HttpStatus.CREATED.value()).contains(response.code)) {
      throw SepException(responseBody)
    }

    return responseBody
  }
}
