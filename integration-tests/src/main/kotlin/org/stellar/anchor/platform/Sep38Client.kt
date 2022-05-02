package org.stellar.anchor.platform

import java.time.Instant
import java.time.format.DateTimeFormatter
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.stellar.anchor.api.sep.sep38.GetPriceResponse
import org.stellar.anchor.api.sep.sep38.GetPricesResponse
import org.stellar.anchor.api.sep.sep38.InfoResponse
import org.stellar.anchor.api.sep.sep38.Sep38QuoteResponse

class Sep38Client(private val endpoint: String, private val jwt: String) : SepClient() {
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

  fun postQuote(
    sellAsset: String,
    sellAmount: String,
    buyAsset: String,
    expireAfter: Instant? = null
  ): Sep38QuoteResponse {
    // build URL
    val urlBuilder = this.endpoint.toHttpUrl().newBuilder().addPathSegment("quote")
    println(urlBuilder.build().toString())

    // build request body
    val requestMap =
      hashMapOf(
        "sell_asset" to sellAsset,
        "sell_amount" to sellAmount,
        "buy_asset" to buyAsset,
      )
    if (expireAfter != null)
      requestMap["expire_after"] = DateTimeFormatter.ISO_INSTANT.format(expireAfter)
    val requestBody = gson.toJson(requestMap).toRequestBody(TYPE_JSON)

    val request =
      Request.Builder()
        .url(urlBuilder.build())
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .post(requestBody)
        .build()
    val response = client.newCall(request).execute()
    val responseBody = handleResponse(response)
    return gson.fromJson(responseBody, Sep38QuoteResponse::class.java)
  }

  fun getQuote(quoteId: String): Sep38QuoteResponse {
    // build URL
    val urlBuilder =
      this.endpoint.toHttpUrl().newBuilder().addPathSegment("quote").addPathSegment(quoteId)
    println(urlBuilder.build().toString())

    val request =
      Request.Builder()
        .url(urlBuilder.build())
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer $jwt")
        .get()
        .build()
    val response = client.newCall(request).execute()
    val responseBody = handleResponse(response)
    return gson.fromJson(responseBody, Sep38QuoteResponse::class.java)
  }
}
