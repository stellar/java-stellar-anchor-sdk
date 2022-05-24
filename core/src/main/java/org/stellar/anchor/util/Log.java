package org.stellar.anchor.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.anchor.config.PII;
import org.stellar.anchor.config.Secret;

/** Logging utility functions. */
public class Log {
  static final Gson gson;

  static {
    LogExclusionStrategy strategy = new LogExclusionStrategy();
    gson = GsonUtils.builder().setExclusionStrategies(strategy).create();
  }

  /**
   * Send debug log.
   *
   * @param message the debug message.
   */
  public static void debug(final String message) {
    logMessageWithJson(message, null, getLogger()::debug);
  }

  /**
   * Send the msg as DEBUG log and detail as JSON.
   *
   * @param message the debug message.
   * @param detail The additional object to be logged.
   */
  public static void debug(final String message, final Object detail) {
    logMessageWithJson(message, detail, getLogger()::debug);
  }

  /**
   * Send detail to INFO log in JSON format.
   *
   * @param detail The additional object to be logged.
   */
  public static void debug(final Object detail) {
    logMessageWithJson(null, detail, getLogger()::debug);
  }

  /**
   * Send debug log with a specified format.
   *
   * @param format The format
   * @param args The arguments of the format
   */
  public static void debugF(final String format, final Object... args) {
    Logger logger = getLogger();
    logger.debug(format, args);
  }

  /**
   * Send message to ERROR log.
   *
   * @param msg The message
   */
  public static void error(final String msg) {
    Logger logger = getLogger();
    logger.error(msg);
  }

  /**
   * Send msg as ERROR log and detail in JSON format.
   *
   * @param message the debug message.
   * @param detail The additional object to be logged.
   */
  public static void error(final String message, final Object detail) {
    logMessageWithJson(message, detail, getLogger()::error);
  }

  /**
   * Send detail to ERROR log in JSON format.
   *
   * @param detail The additional object to be logged.
   */
  public static void error(final Object detail) {
    logMessageWithJson(null, detail, getLogger()::error);
  }

  /**
   * Send exception ERROR log.
   *
   * @param ex The exception.
   */
  public static void errorEx(final Throwable ex) {
    errorEx(null, ex);
  }

  /**
   * Send exception ERROR log with a message.
   *
   * @param ex The exception.
   */
  public static void errorEx(final String msg, final Throwable ex) {
    Logger logger = getLogger();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    if (msg != null) {
      pw.println(msg);
    }
    ex.printStackTrace(pw);
    logger.error(sw.toString());
  }

  /**
   * Send error log with a specified format.
   *
   * @param format The format
   * @param args The arguments of the format
   */
  public static void errorF(final String format, final Object... args) {
    Logger logger = getLogger();
    logger.error(format, args);
  }

  /**
   * Send msg as INFO log.
   *
   * @param message the debug message.
   */
  public static void info(final String message) {
    logMessageWithJson(message, null, getLogger()::info);
  }

  /**
   * Send msg as INFO log and detail in JSON format.
   *
   * @param message the debug message.
   * @param detail The additional object to be logged.
   */
  public static void info(final String message, final Object detail) {
    logMessageWithJson(message, detail, getLogger()::info);
  }

  /**
   * Send detail to INFO log in JSON format.
   *
   * @param detail The additional object to be logged.
   */
  public static void info(final Object detail) {
    logMessageWithJson(null, detail, getLogger()::info);
  }

  /**
   * Send INFO log with format.
   *
   * @param format The format
   * @param args The arguments of the format
   */
  public static void infoF(final String format, final Object... args) {
    Logger logger = getLogger();
    logger.info(format, args);
  }

  /**
   * Return shorter version of the account.
   *
   * @param account The Stellar account Id.
   * @return The shorter version.
   */
  public static String shorter(final String account) {
    if (account.length() > 11) {
      return account.substring(0, 4) + "..." + account.substring(account.length() - 4);
    } else {
      return account;
    }
  }

  /**
   * Send TRACE log.
   *
   * @param message the trace message.
   */
  public static void trace(final String message) {
    logMessageWithJson(message, null, getLogger()::trace);
  }

  /**
   * Send the msg as TRACE log and detail as JSON.
   *
   * @param message the trace message.
   * @param detail The additional object to be logged.
   */
  public static void trace(final String message, Object detail) {
    logMessageWithJson(message, detail, getLogger()::trace);
  }

  /**
   * Send detail to TRACE log in JSON format.
   *
   * @param detail The additional object to be logged.
   */
  public static void trace(final Object detail) {
    logMessageWithJson(null, detail, getLogger()::trace);
  }

  /**
   * Send TRACE log with format.
   *
   * @param format The format
   * @param args The arguments of the format
   */
  public static void traceF(final String format, final Object... args) {
    Logger logger = getLogger();
    logger.trace(format, args);
  }

  /**
   * Send message to WARN log.
   *
   * @param message The message
   */
  public static void warn(final String message) {
    logMessageWithJson(message, null, getLogger()::warn);
  }

  /**
   * Send the msg as WARN log and detail as JSON.
   *
   * @param message the warn message.
   * @param detail The additional object to be logged.
   */
  public static void warn(final String message, Object detail) {
    logMessageWithJson(message, detail, getLogger()::warn);
  }

  /**
   * Send detail to WARN log in JSON format.
   *
   * @param detail The additional object to be logged.
   */
  public static void warn(final Object detail) {
    logMessageWithJson(null, detail, getLogger()::warn);
  }

  /**
   * Send exception WARN log.
   *
   * @param ex The exception.
   */
  public static void warnEx(final Throwable ex) {
    Logger logger = getLogger();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ex.printStackTrace(pw);
    logger.warn(sw.toString());
  }

  /**
   * Send WARN log with format.
   *
   * @param format The format
   * @param args The arguments of the format
   */
  public static void warnF(final String format, final Object... args) {
    Logger logger = getLogger();
    logger.warn(format, args);
  }

  static Logger getLogger() {
    String cls = Thread.currentThread().getStackTrace()[3].getClassName();
    return LoggerFactory.getLogger(cls);
  }

  static void logMessageWithJson(
      final String message, final Object detail, final Consumer<String> output) {
    StringBuilder sb = new StringBuilder();
    if (message != null) {
      sb.append(message);
    }
    if (detail != null) {
      sb.append(gson.toJson(detail));
    }
    output.accept(sb.toString());
  }
}

class LogExclusionStrategy implements ExclusionStrategy {
  @Override
  public boolean shouldSkipField(FieldAttributes f) {
    // Skip if the field is annotated
    if (f.getAnnotation(PII.class) != null || f.getAnnotation(Secret.class) != null) {
      return true;
    }

    String readMethodName = String.format("get%s", StringUtils.capitalize(f.getName()));
    try {
      // Skip if the readMethod is annotated
      Class<?> cls = f.getDeclaringClass();
      Method readMethod = cls.getMethod(readMethodName);
      if (shouldSkipMethod(readMethod)) {
        return true;
      }

      // Skip if the readMethod of any class-implementing interface is annotated
      for (Class<?> ifc : cls.getInterfaces()) {
        readMethod = ifc.getMethod(readMethodName);
        if (shouldSkipMethod(readMethod)) {
          return true;
        }
      }
    } catch (NoSuchMethodException e) {
      // the field does not have a get method
    }

    return false;
  }

  @Override
  public boolean shouldSkipClass(Class<?> clazz) {
    return false;
  }

  boolean shouldSkipMethod(Method method) {
    return (method.isAnnotationPresent(PII.class) || method.isAnnotationPresent(Secret.class));
  }
}
