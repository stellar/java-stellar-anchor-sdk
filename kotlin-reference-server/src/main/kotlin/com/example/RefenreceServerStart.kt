package com.example

// Used in ServiceRunner
@JvmOverloads // Annotation required to call from Java with optional argument
fun start(port: Int, waitServer: Boolean = false) =
  main(arrayOf(port.toString(), waitServer.toString()))
