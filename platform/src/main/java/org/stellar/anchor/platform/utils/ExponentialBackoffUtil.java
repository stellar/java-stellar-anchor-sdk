package org.stellar.anchor.platform.utils;

import org.stellar.anchor.util.Log;

/**
 * ExponentialBackoffUtil is used to do an exponential back-off, where the sleep time is doubled
 * every time, until things succeed and the `resetSleepSeconds()` method is called.
 */
public class ExponentialBackoffUtil {
  private static final long DEFAULT_INITIAL_SLEEP_SECONDS = 1;
  private static final long DEFAULT_MAX_SLEEP_SECONDS = 300; // 5 minutes

  private final long initialSleepSeconds;
  private final long maxSleepSeconds;
  private long sleepSeconds;

  public ExponentialBackoffUtil() {
    this(DEFAULT_INITIAL_SLEEP_SECONDS, DEFAULT_MAX_SLEEP_SECONDS);
  }

  public ExponentialBackoffUtil(long initialSleepSeconds, long maxSleepSeconds) {
    if (initialSleepSeconds < 1) {
      throw new IllegalArgumentException(
          "The formula 'initialSleepSeconds >= 1' is not being respected.");
    }
    this.initialSleepSeconds = initialSleepSeconds;

    if (maxSleepSeconds < initialSleepSeconds) {
      throw new IllegalArgumentException(
          "The formula 'maxSleepSeconds >= initialSleepSeconds' is not being respected.");
    }
    this.maxSleepSeconds = maxSleepSeconds;
  }

  public void increaseSleepSeconds() {
    sleepSeconds = Long.max(sleepSeconds * 2, maxSleepSeconds);
  }

  public void resetSleepSeconds() {
    sleepSeconds = initialSleepSeconds;
  }

  public void sleep() {
    try {
      wait(sleepSeconds * 1000);
    } catch (InterruptedException ex) {
      Log.errorEx(ex);
    }
  }
}
