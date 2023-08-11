package org.stellar.anchor.platform

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.net.URLDecoder
import java.nio.file.Files
import java.util.*
import java.util.jar.JarFile
import org.stellar.anchor.util.Log.info

/** The temporary folder where resources are extracted to. */
val resourceTempDir = extractResourcesToTempFolder()

/**
 * Reads a .env file and returns a map of the key value pairs.
 *
 * @param resourceName The name of the resource file.
 * @return A map of the key value pairs.
 */
fun readResourceAsMap(resourceName: String): MutableMap<String, String> {
  return readResourceAsMap(getResourceFile(resourceName))
}

/**
 * Gets a resource file from the jar file or the classpath.
 *
 * @param resourceName The name of the resource file.
 * @return The resource file.
 */
fun getResourceFile(resourceName: String): File {
  val fixedResourcePath =
    if (resourceName.startsWith("/")) resourceName.substring(1) else resourceName
  return if (resourceTempDir != null) {
    File(resourceTempDir, resourceName)
  } else {
    val resourceUrl: URL? = {}::class.java.classLoader.getResource(fixedResourcePath)
    File(resourceUrl!!.toURI())
  }
}

/**
 * Reads a .env file and returns a map of the key value pairs.
 *
 * @param file The .env file to read.
 * @return A map of the key value pairs.
 */
fun readResourceAsMap(file: File): MutableMap<String, String> {
  val prop = Properties()
  prop.load(file.inputStream())
  return prop.entries.associate { it.key.toString() to it.value.toString() }.toMutableMap()
}

/**
 * Extracts resources from the jar file to a temporary folder.
 *
 * @return The temporary folder where resources are extracted to.
 *
 * ```
 *        Returns null if the application is not running from a jar file.
 * ```
 */
fun extractResourcesToTempFolder(): File? {
  if (isRunningFromJar()) {
    val jarFile = getJarFile()
    val tempDir = Files.createTempDirectory("resource-temp-dir").toFile()
    info("Extacting resources to temp folder ${tempDir.absolutePath}...")
    tempDir.deleteOnExit()
    val jar = JarFile(jarFile)
    val entries = jar.entries()
    while (entries.hasMoreElements()) {
      val entry = entries.nextElement()
      val startsWithPrefix: String? =
        startsWithPrefixSet(entry.name, setOf("..", "BOOT-INF/classes", "classes"))
      val entryName =
        if (startsWithPrefix == null) entry.name else entry.name.substring(startsWithPrefix.length)
      if (entryName.startsWith("..")) continue
      val file = File(tempDir, entryName)
      if (entry.isDirectory) {
        file.mkdirs()
      } else {
        file.parentFile.mkdirs()
        val fis = jar.getInputStream(entry)
        val fos = FileOutputStream(file)
        copyStream(fis, fos)
        fis.close()
        fos.close()
      }
    }
    return tempDir
  } else {
    return null
  }
}

fun startsWithPrefixSet(input: String, prefixes: Set<String>): String? {
  for (prefix in prefixes) {
    if (input.startsWith(prefix)) {
      return prefix
    }
  }
  return null
}

fun copyStream(inputStream: InputStream, outputStream: OutputStream) {
  val buffer = ByteArray(1024)
  var bytesRead: Int
  while (inputStream.read(buffer).also { bytesRead = it } != -1) {
    outputStream.write(buffer, 0, bytesRead)
  }
  outputStream.flush()
}

/** Check if the application is running from a jar file. */
fun isRunningFromJar(): Boolean {
  val className = ServiceRunner::class.java.name.replace('.', '/')
  val classJar = ServiceRunner::class.java.getResource("/${className}.class")?.toString()
  return classJar?.startsWith("jar:") ?: false
}

/**
 * Get the jar file that the application is running from.
 *
 * @return The jar file or null if the application is not running from a jar file.
 */
fun getJarFile(): File? {
  val className = ServiceRunner::class.java.name.replace('.', '/')
  val classUrl = ServiceRunner::class.java.classLoader.getResource("$className.class")

  return if (classUrl != null && "jar" == classUrl.protocol) {
    val jarPath = classUrl.path.substring(5, classUrl.path.indexOf('!'))
    File(URLDecoder.decode(jarPath, "UTF-8"))
  } else {
    null
  }
}
