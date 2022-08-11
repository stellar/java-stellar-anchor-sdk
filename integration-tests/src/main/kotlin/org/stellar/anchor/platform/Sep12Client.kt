package org.stellar.anchor.platform

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpStatus
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.sep.sep12.Sep12DeleteCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerResponse
import shadow.com.google.common.reflect.TypeToken

const val APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8"
val TYPE_JSON = APPLICATION_JSON_CHARSET_UTF_8.toMediaType()
const val MULTIPART_FORM_DATA_CHARSET_UTF_8 = "multipart/form-data; charset=utf-8"
val TYPE_MULTIPART_FORM_DATA = MULTIPART_FORM_DATA_CHARSET_UTF_8.toMediaType()

class Sep12Client(private val endpoint: String, private val jwt: String) : SepClient() {
  fun getCustomer(id: String, type: String? = null): Sep12GetCustomerResponse? {
    var url = String.format(this.endpoint + "/customer?id=%s", id)
    if (type != null) {
      url += "&type=$type"
    }
    val responseBody = httpGet(url, jwt)
    return gson.fromJson(responseBody, Sep12GetCustomerResponse::class.java)
  }

  fun putCustomer(
    customerRequest: Sep12PutCustomerRequest,
    mediaType: MediaType = TYPE_JSON
  ): Sep12PutCustomerResponse? {
    val body: RequestBody?
    when (mediaType) {
      TYPE_JSON -> {
        body = gson.toJson(customerRequest).toRequestBody(mediaType)
      }
      TYPE_MULTIPART_FORM_DATA -> {
        var multipartBuilder = MultipartBody.Builder().setType(TYPE_MULTIPART_FORM_DATA)

        val type = object : TypeToken<Map<String, String>>() {}.type
        val parametersMap: Map<String, String> = gson.fromJson(gson.toJson(customerRequest), type)
        parametersMap.forEach { (key, value) ->
          multipartBuilder = multipartBuilder.addFormDataPart(key, value)
        }

        body = multipartBuilder.build()
      }
      else -> {
        throw RuntimeException("Unsupported type $mediaType")
      }
    }

    val request =
      Request.Builder()
        .url(this.endpoint + "/customer")
        .header("Authorization", "Bearer $jwt")
        .put(body)
        .build()
    val response = client.newCall(request).execute()
    if (response.code == HttpStatus.FORBIDDEN.value()) {
      throw SepNotAuthorizedException("Forbidden")
    }

    assert(response.code == HttpStatus.ACCEPTED.value())

    return gson.fromJson(response.body!!.string(), Sep12PutCustomerResponse::class.java)
  }

  fun deleteCustomer(account: String): Int {
    val deleteCustomerRequest = Sep12DeleteCustomerRequest.builder().account(account).build()

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

  /**
   * ATTENTION: this function is used for testing purposes only.
   *
   * <p>This endpoint is used to delete a customer's `clabe_number`, which would make its state
   * change to NEEDS_INFO if it's a receiving customer.
   */
  fun invalidateCustomerClabe(id: String) {
    val url = String.format("http://localhost:8081/invalidate_clabe/%s", id)
    httpGet(url, jwt)
  }
}
