package org.stellar.anchor.platform

import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.springframework.http.HttpStatus
import org.stellar.anchor.dto.sep38.GetPriceResponse
import org.stellar.anchor.dto.sep38.GetPricesResponse
import org.stellar.anchor.dto.sep38.InfoResponse
import org.stellar.anchor.dto.sep38.Sep38QuoteResponse
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

  fun getPrice(sellAsset: String, sellAmount: String, buyAsset: String): GetPriceResponse {
    // build URL
    val urlBuilder =
      this.endpoint
        .toHttpUrl()
        .newBuilder()
        .addPathSegment("price")
        .addQueryParameter("sell_asset", sellAsset)
        .addQueryParameter("sell_amount", sellAmount)
        .addQueryParameter("buy_asset", buyAsset)
    println(urlBuilder.build().toString())

    val request =
      Request.Builder()
        .url(urlBuilder.build())
        .header("Content-Type", "application/json")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val responseBody = handleResponse(response)
    return gson.fromJson(responseBody, GetPriceResponse::class.java)
  }

  fun postQuote(sellAsset: String, sellAmount: String, buyAsset: String): Sep38QuoteResponse {
    // build URL
    val urlBuilder = this.endpoint.toHttpUrl().newBuilder().addPathSegment("quote")
    println(urlBuilder.build().toString())

    // build request body
    val requestBody =
      """{
      "sell_asset": "$sellAsset",
      "sell_amount": "$sellAmount",
      "buy_asset": "$buyAsset"
    }"""
        .trimIndent()
        .toRequestBody(TYPE_JSON)

    val request =
      Request.Builder()
        .url(urlBuilder.build())
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer $jwtStr")
        .post(requestBody)
        .build()
    val response = client.newCall(request).execute()
    val responseBody = handleResponse(response)
    return gson.fromJson(responseBody, Sep38QuoteResponse::class.java)
  }

  private fun handleResponse(response: Response): String? {
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
