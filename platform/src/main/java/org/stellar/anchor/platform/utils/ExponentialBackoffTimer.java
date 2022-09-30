package org.stellar.anchor.platform.utils;

/**
 * ExponentialBackoffUtil is used to do an exponential back-off, where the sleep time is doubled
 * every time, until things succeed and the `resetSleepSeconds()` method is called.
 */
public class ExponentialBackoffTimer {
  private static final long DEFAULT_INITIAL_SLEEP_SECONDS = 1;
  private static final long DEFAULT_MAX_SLEEP_SECONDS = 300; // 5 minutes

  private final long initialSleepSeconds;
  private final long maxSleepSeconds;
  private long sleepSeconds;

  public ExponentialBackoffTimer() {
    this(DEFAULT_INITIAL_SLEEP_SECONDS, DEFAULT_MAX_SLEEP_SECONDS);
  }

  public ExponentialBackoffTimer(long initialSleepSeconds, long maxSleepSeconds) {
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

  public void increase() {
    sleepSeconds = Long.max(sleepSeconds * 2, maxSleepSeconds);
  }

  public void reset() {
    sleepSeconds = initialSleepSeconds;
  }

  public void sleep() throws InterruptedException {
    wait(sleepSeconds * 1000);
  }
}
