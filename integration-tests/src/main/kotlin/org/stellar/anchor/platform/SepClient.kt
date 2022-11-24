package org.stellar.anchor.platform

import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.springframework.http.HttpStatus
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.api.sep.SepExceptionResponse
import org.stellar.anchor.util.GsonUtils

class SepClient {
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

  fun httpGet(url: String, jwt: String? = null): String? {
    var builder = Request.Builder().url(url).header("Content-Type", "application/json").get()
    if (jwt != null) {
      builder = builder.header("Authorization", "Bearer $jwt")
    }
    val request = builder.build()

    val response = client.newCall(request).execute()
    return handleResponse(response)
  }

  fun httpPost(url: String, requestBody: Map<String, Any>, jwt: String? = null): String? {
    val requestBodyStr = gson.toJson(requestBody).toRequestBody(TYPE_JSON)

    var builder =
      Request.Builder().url(url).header("Content-Type", "application/json").post(requestBodyStr)
    if (jwt != null) {
      builder = builder.header("Authorization", "Bearer $jwt")
    }
    val request = builder.build()

    val response = client.newCall(request).execute()
    return handleResponse(response)
  }

  fun handleResponse(response: Response): String? {
    val responseBody = response.body?.string()

    println("statusCode: " + response.code)
    println("responseBody: $responseBody")
    when (response.code) {
      HttpStatus.OK.value(),
      HttpStatus.CREATED.value(),
      HttpStatus.ACCEPTED.value() -> return responseBody
      HttpStatus.FORBIDDEN.value() -> throw SepNotAuthorizedException("Forbidden")
      HttpStatus.NOT_FOUND.value() -> {
        val sepException = gson.fromJson(responseBody, SepExceptionResponse::class.java)
        throw SepNotFoundException(sepException.error)
      }
      else -> throw SepException(responseBody)
    }
  }
}
