package com.example

// Used in ServiceRunner
@JvmOverloads // Annotation required to call from Java with optional argument
fun start(waitServer: Boolean = false) = main(arrayOf(waitServer.toString()))
