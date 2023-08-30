package org.stellar.reference

// Used in ServiceRunner
@JvmOverloads // Annotation required to call from Java with optional argument
fun start(envMap: Map<String, String>?, waitServer: Boolean = false) =
  startServer(envMap, waitServer)

fun stop() = stopServer()
