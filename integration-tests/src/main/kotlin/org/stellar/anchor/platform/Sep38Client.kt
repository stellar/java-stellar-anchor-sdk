package org.stellar.anchor.platform

import java.time.Instant
import java.time.format.DateTimeFormatter
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.stellar.anchor.api.sep.sep38.GetPriceResponse
import org.stellar.anchor.api.sep.sep38.GetPricesResponse
import org.stellar.anchor.api.sep.sep38.InfoResponse
import org.stellar.anchor.api.sep.sep38.Sep38QuoteResponse

class Sep38Client(private val endpoint: String, private val jwt: String) : SepClient() {
  fun getInfo(): InfoResponse {
    println("$endpoint/info")
    val responseBody = httpGet("$endpoint/info")
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

    val responseBody = httpGet(urlBuilder.build().toString())
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

    val responseBody = httpGet(urlBuilder.build().toString())
    return gson.fromJson(responseBody, GetPriceResponse::class.java)
  }

  fun postQuote(
    sellAsset: String,
    sellAmount: String,
    buyAsset: String,
    expireAfter: Instant? = null
  ): Sep38QuoteResponse {
    // build URL
    println("$endpoint/quote")

    // build request body
    val requestBody =
      hashMapOf(
        "sell_asset" to sellAsset,
        "sell_amount" to sellAmount,
        "buy_asset" to buyAsset,
      )
    if (expireAfter != null) {
      requestBody["expire_after"] = DateTimeFormatter.ISO_INSTANT.format(expireAfter)
    }

    val responseBody = httpPost("$endpoint/quote", requestBody, jwt)
    return gson.fromJson(responseBody, Sep38QuoteResponse::class.java)
  }

  fun getQuote(quoteId: String): Sep38QuoteResponse {
    // build URL
    val urlBuilder =
      this.endpoint.toHttpUrl().newBuilder().addPathSegment("quote").addPathSegment(quoteId)
    println(urlBuilder.build().toString())

    val responseBody = httpGet(urlBuilder.build().toString(), jwt)
    return gson.fromJson(responseBody, Sep38QuoteResponse::class.java)
  }
}
