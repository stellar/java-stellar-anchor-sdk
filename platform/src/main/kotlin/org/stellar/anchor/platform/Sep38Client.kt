package org.stellar.anchor.platform

import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.springframework.http.HttpStatus
import org.stellar.anchor.dto.sep38.GetPricesResponse
import org.stellar.anchor.dto.sep38.InfoResponse
import org.stellar.anchor.exception.SepException
import org.stellar.anchor.exception.SepNotAuthorizedException

class Sep38Client(private val endpoint: String) : SepClient() {
  private val gson = Gson()

  fun getInfo(): InfoResponse {
    println("$endpoint/info")
    val request =
      Request.Builder()
        .url("$endpoint/info")
        .header("Content-Type", "application/json")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val responseBody = handleResponse(response)
    return gson.fromJson(responseBody, InfoResponse::class.java)
  }

  fun getPrices(sellAsset: String, sellAmount: String): GetPricesResponse {
    // build URL
    val urlBuilder =
      this.endpoint
        .toHttpUrl()
        .newBuilder()
        .addPathSegment("prices")
        .addQueryParameter("sell_asset", sellAsset)
        .addQueryParameter("sell_amount", sellAmount)
    println(urlBuilder.build().toString())

    val request =
      Request.Builder()
        .url(urlBuilder.build())
        .header("Content-Type", "application/json")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val responseBody = handleResponse(response)
    return gson.fromJson(responseBody, GetPricesResponse::class.java)
  }

  private fun handleResponse(response: Response): String? {
    val responseBody = response.body?.string()

    println("responseBody: $responseBody")
    if (response.code == HttpStatus.FORBIDDEN.value()) {
      throw SepNotAuthorizedException("Forbidden")
    } else if (response.code != HttpStatus.OK.value()) {
      throw SepException(responseBody)
    }

    return responseBody
  }
}
