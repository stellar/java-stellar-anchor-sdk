package org.stellar.anchor.platform

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpStatus
import org.stellar.anchor.dto.sep12.Sep12DeleteCustomerRequest
import org.stellar.anchor.dto.sep12.Sep12GetCustomerResponse
import org.stellar.anchor.dto.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.dto.sep12.Sep12PutCustomerResponse
import org.stellar.anchor.exception.SepNotAuthorizedException

const val HOST_URL = "http://localhost:8080"
const val APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8"
val TYPE_JSON = APPLICATION_JSON_CHARSET_UTF_8.toMediaTypeOrNull()

class Sep12Client(private val endpoint: String, private val jwt: String) : SepClient() {
  fun getCustomer(id: String): Sep12GetCustomerResponse? {
    val request =
      Request.Builder()
        .url(String.format(this.endpoint + "/customer?id=%s", id))
        .header("Authorization", "Bearer $jwt")
        .get()
        .build()

    val response = client.newCall(request).execute()
    if (response.code == HttpStatus.FORBIDDEN.value()) {
      throw SepNotAuthorizedException("Forbidden")
    }
    val responseBody = response.body!!.string()
    return gson.fromJson(responseBody, Sep12GetCustomerResponse::class.java)
  }

  fun putCustomer(customerRequest: Sep12PutCustomerRequest): Sep12PutCustomerResponse? {
    val request =
      Request.Builder()
        .url(this.endpoint + "/customer")
        .header("Authorization", "Bearer $jwt")
        .put(gson.toJson(customerRequest).toRequestBody(TYPE_JSON))
        .build()
    val response = client.newCall(request).execute()
    if (response.code == HttpStatus.FORBIDDEN.value()) {
      throw SepNotAuthorizedException("Forbidden")
    }

    assert(response.code == HttpStatus.ACCEPTED.value())

    return gson.fromJson(response.body!!.string(), Sep12PutCustomerResponse::class.java)
  }

  fun deleteCustomer(account: String): Int {
    val deleteCustomerRequest = Sep12DeleteCustomerRequest()
    deleteCustomerRequest.account = account

    val request =
      Request.Builder()
        .url(this.endpoint + "/customer/${deleteCustomerRequest.account}")
        .header("Authorization", "Bearer $jwt")
        .delete(gson.toJson(deleteCustomerRequest).toRequestBody(TYPE_JSON))
        .build()
    val response = client.newCall(request).execute()
    if (response.code == HttpStatus.FORBIDDEN.value()) {
      throw SepNotAuthorizedException("Forbidden")
    }
    return response.code
  }
}
