package org.stellar.anchor.platform.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This class is used to create daemon threads. Daemon threads are threads that do not prevent the
 * JVM from exiting when the program finishes but the thread is still running.
 */
public class DaemonThreadFactory implements ThreadFactory {
  @Override
  public Thread newThread(Runnable r) {
    Thread thread = Executors.defaultThreadFactory().newThread(r);
    thread.setDaemon(true); // Set the thread as a daemon thread
    return thread;
  }
}
