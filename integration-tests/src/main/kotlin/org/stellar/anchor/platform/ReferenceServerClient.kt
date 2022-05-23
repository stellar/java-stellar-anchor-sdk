package org.stellar.anchor.platform

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

class ReferenceServerClient(private val endpoint: String) : SepClient() {
  fun health(checks: List<String>): java.util.HashMap<*, *> {
    var url = endpoint.toHttpUrlOrNull()
    val bldr =
      HttpUrl.Builder().scheme(url!!.scheme).host(url.host).port(url.port).addPathSegment("health")

    if (checks.isNotEmpty()) {
      bldr.addQueryParameter("checks", checks.joinToString())
    }

    url = bldr.build()

    val request =
      Request.Builder().url(url).header("Content-Type", "application/json").get().build()
    val response = client.newCall(request).execute()
    return gson.fromJson(handleResponse(response), HashMap::class.java)
  }
}
