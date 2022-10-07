package org.stellar.anchor.util

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ExponentialBackoffTimerTest {
  @Test
  fun test_constructorWithParameters() {
    // validate if 'initialSleepSeconds >= 1'
    var ex: IllegalArgumentException = assertThrows { ExponentialBackoffTimer(0, 0) }
    assertEquals("The formula 'initialSleepSeconds >= 1' is not being respected.", ex.message)

    // validate if 'maxSleepSeconds >= initialSleepSeconds'
    ex = assertThrows { ExponentialBackoffTimer(1, 0) }
    assertEquals(
      "The formula 'maxSleepSeconds >= initialSleepSeconds' is not being respected.",
      ex.message
    )

    // constructor with all parameters works
    lateinit var exponentialBackoffTimer: ExponentialBackoffTimer
    assertDoesNotThrow { exponentialBackoffTimer = ExponentialBackoffTimer(1, 2) }
    assertEquals(1, exponentialBackoffTimer.initialSleepSeconds)
    assertEquals(2, exponentialBackoffTimer.maxSleepSeconds)

    // constructor with no parameters works
    assertDoesNotThrow { exponentialBackoffTimer = ExponentialBackoffTimer() }
    assertEquals(
      ExponentialBackoffTimer.DEFAULT_INITIAL_SLEEP_SECONDS,
      exponentialBackoffTimer.initialSleepSeconds
    )
    assertEquals(
      ExponentialBackoffTimer.DEFAULT_MAX_SLEEP_SECONDS,
      exponentialBackoffTimer.maxSleepSeconds
    )
  }

  @Test
  fun test_increase() {
    val exponentialBackoffTimer = ExponentialBackoffTimer(1, 5)
    assertEquals(1, exponentialBackoffTimer.sleepSeconds)

    exponentialBackoffTimer.increase()
    assertEquals(2, exponentialBackoffTimer.sleepSeconds)

    exponentialBackoffTimer.increase()
    assertEquals(4, exponentialBackoffTimer.sleepSeconds)

    exponentialBackoffTimer.increase()
    assertEquals(5, exponentialBackoffTimer.sleepSeconds)

    exponentialBackoffTimer.increase()
    assertEquals(5, exponentialBackoffTimer.sleepSeconds)
  }

  @Test
  fun test_reset() {
    val exponentialBackoffTimer = ExponentialBackoffTimer(1, 5)
    exponentialBackoffTimer.increase()
    assertEquals(2, exponentialBackoffTimer.sleepSeconds)

    exponentialBackoffTimer.reset()
    assertEquals(1, exponentialBackoffTimer.sleepSeconds)
  }

  @Test
  fun test_sleep() {
    val exponentialBackoffTimer = ExponentialBackoffTimer(1, 5)

    val beforeSleep = Instant.now()
    exponentialBackoffTimer.sleep()
    val afterSleep = Instant.now()

    assertEquals(1, ChronoUnit.SECONDS.between(beforeSleep, afterSleep))
  }
}
