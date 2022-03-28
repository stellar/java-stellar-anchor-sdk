package org.stellar.anchor.platform

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.util.FileCopyUtils
import org.stellar.anchor.util.InstantConverter

val gson: Gson =
  GsonBuilder()
    .registerTypeAdapter(Instant::class.java, InstantConverter())
    .setPrettyPrinting()
    .create()

fun json(value: Any?): String {
  if (value != null) return gson.toJson(value)
  return "null"
}

val resourceLoader = DefaultResourceLoader()

fun resourceAsString(path: String): String {
  val reader = InputStreamReader(resourceLoader.getResource(path).inputStream, UTF_8)
  return FileCopyUtils.copyToString(reader)
}

fun printRequest(title: String?, payload: Any? = null) {
  if (title != null) println(title)
  if (payload != null) {
    print("request=")
    println(if (payload is String) payload else json(payload))
  }
}

fun printResponse(payload: Any?) {
  printResponse(null, payload)
}

fun printResponse(title: String?, payload: Any?) {
  if (title != null) println(title)
  if (payload != null) {
    print("response=")
    println(if (payload is String) payload else json(payload))
  }
  println()
}
