package org.stellar.anchor.platform

import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

open class SepClient {
  companion object {
    val gson = Gson()
    val client =
      OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()
  }
}
