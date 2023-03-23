package org.stellar.anchor.platform

import io.github.cdimascio.dotenv.internal.DotenvParser
import io.github.cdimascio.dotenv.internal.DotenvReader
import java.io.File
import java.net.URL

fun readResourceAsMap(resourcePath: String): MutableMap<String, String> {
  val resourceFilePath = getResourceFilePath(resourcePath)
  val resourceFile = File(resourceFilePath)

  return DotenvParser(
      DotenvReader(resourceFile.parentFile.absolutePath, resourceFile.name),
      false,
      false
    )
    .parse()
    .associate { it.key to it.value }
    .toMutableMap()
}

fun getResourceFilePath(resourcePath: String): String {
  val resourceUrl: URL? =
    {}::class
      .java
      .classLoader
      .getResource(if (resourcePath.startsWith("/")) resourcePath.substring(1) else resourcePath)
  val resourceFile = File(resourceUrl!!.toURI())
  return resourceFile.absolutePath
}
