package org.stellar.anchor.platform

import io.github.cdimascio.dotenv.internal.DotenvParser
import io.github.cdimascio.dotenv.internal.DotenvReader
import java.io.File
import java.net.URL

fun readResourceAsMap(resourcePath: String): MutableMap<String, String> {
  val resourceUrl: URL? = {}::class.java.classLoader.getResource(resourcePath)
  val resourceFile = File(resourceUrl!!.toURI())

  return DotenvParser(
          DotenvReader(resourceFile.parentFile.absolutePath, resourceFile.name), false, false)
      .parse()
      .associate { it.key to it.value }
      .toMutableMap()
}

fun getResourceFilePath(resourcePath: String): String {
  val resourceUrl: URL? = {}::class.java.classLoader.getResource(resourcePath)
  val resourceFile = File(resourceUrl!!.toURI())
  return resourceFile.absolutePath
}
