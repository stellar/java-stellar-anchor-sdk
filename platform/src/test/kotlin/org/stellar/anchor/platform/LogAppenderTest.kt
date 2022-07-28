package org.stellar.anchor.platform

import java.io.*
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.*
import org.stellar.anchor.util.*

class LogAppenderTest {
  @Test
  fun test_logJsonAppenderFormat() {
    val outputStreamCaptor = ByteArrayOutputStream()
    System.setOut(PrintStream(outputStreamCaptor))
    Log.info("hello world")
    val outputStream = outputStreamCaptor.toString().trim()

    MatcherAssert.assertThat(
      outputStream,
      CoreMatchers.endsWith("INFO  - o.s.a.p.LogAppenderTest - hello world")
    )
  }
}
