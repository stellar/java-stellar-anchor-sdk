package org.stellar.anchor.platform

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.stellar.anchor.util.*

class LogAppenderTest {
  @Test
  fun test_logJsonAppenderFormat() {
    val outputStreamCaptor = ByteArrayOutputStream()
    System.setOut(PrintStream(outputStreamCaptor))
    Log.info("hello world")
    val json = outputStreamCaptor.toString().trim()

    val gson = Gson()
    val type = object : TypeToken<Map<String?, *>?>() {}.type
    var actualLog: Map<String, Any>? = null

    assertDoesNotThrow { actualLog = gson.fromJson(json, type) }
    assertEquals("{message=hello world, severity=INFO}", actualLog!!["event"].toString())
  }
}
