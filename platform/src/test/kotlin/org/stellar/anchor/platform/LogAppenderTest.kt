package org.stellar.anchor.platform

import io.mockk.*
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.stellar.anchor.util.*

class LogAppenderTest {
  @Test
  fun test_loggerInfo_accurateLoggerName() {
    val appender = mockk<Appender>()
    val capturedLogEvent = slot<LogEvent>()

    every { appender.name } returns "mock appender"
    every { appender.isStarted } returns true
    every { appender.append(capture(capturedLogEvent)) }

    val loggerContext: LoggerContext = LogManager.getContext(false) as LoggerContext
    val loggerConfig = loggerContext.configuration.getLoggerConfig("root")
    loggerConfig.addAppender(appender, Level.ALL, null)

    Log.info("hello world")
    assertEquals(LogAppenderTest::class.qualifiedName, capturedLogEvent.captured.loggerName)
  }
}
