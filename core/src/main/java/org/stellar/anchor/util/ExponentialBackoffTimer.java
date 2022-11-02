package org.stellar.anchor.util;

/**
 * ExponentialBackoffUtil is used to do an exponential back-off, where the sleep time is doubled
 * every time, until things succeed and the `resetSleepSeconds()` method is called.
 */
public class ExponentialBackoffTimer {
  static final long DEFAULT_INITIAL_SLEEP_SECONDS = 1;
  static final long DEFAULT_MAX_SLEEP_SECONDS = 300; // 5 minutes

  final long initialSleepSeconds;
  final long maxSleepSeconds;
  long sleepSeconds;

  public ExponentialBackoffTimer() {
    this(DEFAULT_INITIAL_SLEEP_SECONDS, DEFAULT_MAX_SLEEP_SECONDS);
  }

  public ExponentialBackoffTimer(long initialSleepSeconds, long maxSleepSeconds) {
    if (initialSleepSeconds < 1) {
      throw new IllegalArgumentException(
          "The formula 'initialSleepSeconds >= 1' is not being respected.");
    }
    this.initialSleepSeconds = initialSleepSeconds;
    this.sleepSeconds = initialSleepSeconds;

    if (maxSleepSeconds < initialSleepSeconds) {
      throw new IllegalArgumentException(
          "The formula 'maxSleepSeconds >= initialSleepSeconds' is not being respected.");
    }
    this.maxSleepSeconds = maxSleepSeconds;
  }

  public void increase() {
    sleepSeconds = Long.min(sleepSeconds * 2, maxSleepSeconds);
  }

  public void reset() {
    sleepSeconds = initialSleepSeconds;
  }

  public void sleep() throws InterruptedException {
    Thread.sleep(sleepSeconds * 1000);
  }

  public long currentTimer() {
    return sleepSeconds;
  }

  public boolean isTimerMaxed() {
    return sleepSeconds >= maxSleepSeconds;
  }
}
