package org.stellar.anchor.platform.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class DaemonExecutors {
  private static ThreadFactory daemonThreadFactory = new DaemonThreadFactory();

  public static ScheduledExecutorService newScheduledThreadPool(int threadCount) {
    return Executors.newScheduledThreadPool(threadCount, daemonThreadFactory);
  }
}
