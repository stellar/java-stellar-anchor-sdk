package org.stellar.anchor.platform

import com.google.gson.*
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

    assertDoesNotThrow { gson.fromJson(json, JsonLog::class.java) }
    val actualLog = gson.fromJson(json, JsonLog::class.java)
    assertEquals(actualLog.event.message, "hello world")
    assertEquals(actualLog.event.severity, "INFO")
  }
}
