package org.stellar.anchor.platform

import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.stellar.anchor.dto.sep12.Sep12GetCustomerResponse
import org.stellar.anchor.dto.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.dto.sep12.Sep12PutCustomerResponse
import org.stellar.anchor.sep10.JwtService
import org.stellar.anchor.sep10.JwtToken

const val HOST_URL = "http://localhost:8080"
const val TEST_PUBLIC_KEY = "GBJDSMTMG4YBP27ZILV665XBISBBNRP62YB7WZA2IQX2HIPK7ABLF4C2"
const val JWT_SECRET = "secret"
const val APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8"
val TYPE_JSON = APPLICATION_JSON_CHARSET_UTF_8.toMediaTypeOrNull()

class Sep12(private val endpoint: String) {
  companion object {

    val jwtService = JwtService(JWT_SECRET)
    val gson = Gson()
    val client =
      OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()
  }

  fun getCustomer(id: String): Sep12GetCustomerResponse? {
    val jwtToken = createJwtToken()
    val request =
      Request.Builder()
        .url(String.format(this.endpoint + "/customer?id=%s", id))
        .header("Authorization", "Bearer $jwtToken")
        .get()
        .build()

    val response = client.newCall(request).execute()
    val responseBody = response.body!!.string()
    return gson.fromJson(responseBody, Sep12GetCustomerResponse::class.java)
  }

  fun putCustomer(customerRequest: Sep12PutCustomerRequest): Sep12PutCustomerResponse? {
    val jwtToken = createJwtToken()
    val request =
      Request.Builder()
        .url(this.endpoint + "/customer")
        .header("Authorization", "Bearer $jwtToken")
        .put(gson.toJson(customerRequest).toRequestBody(TYPE_JSON))
        .build()
    val response = client.newCall(request).execute()
    return gson.fromJson(response.body!!.string(), Sep12PutCustomerResponse::class.java)
  }

  fun createJwtToken(): String {
    val issuedAt: Long = System.currentTimeMillis() / 1000L
    val jwtToken =
      JwtToken.of(HOST_URL + "/auth", TEST_PUBLIC_KEY, issuedAt, issuedAt + 60, "", null)
    return jwtService.encode(jwtToken)
  }
}
