package org.stellar.anchor.util;

import com.google.gson.Gson;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.anchor.config.PII;
import org.stellar.anchor.config.Secret;

/** Logging utility functions. */
public class Log {
  static final Gson gson = GsonUtils.builder().setPrettyPrinting().create();

  /**
   * Send debug log.
   *
   * @param message the debug message.
   */
  public static void debug(final String message) {
    printPlainText(message, null, getLogger()::debug);
  }

  /**
   * Send the msg as DEBUG log and detail as JSON.
   *
   * @param message the debug message.
   * @param detail The additional object to be logged.
   */
  public static void debug(final String message, Object detail) {
    printPlainText(message, detail, getLogger()::debug);
  }

  /**
   * Send msg as DEBUG log and detail as a Java bean. Ignore properties that are annotated
   * with @PII.
   *
   * @param msg the debug message.
   * @param detail The additional object to be logged.
   */
  public static void debugB(final String msg, final Object detail) {
    Logger logger = getLogger();
    printBeanFormat(msg, detail, logger::debug);
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
  public static void error(String msg) {
    Logger logger = getLogger();
    logger.error(msg);
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
  public static void errorEx(String msg, final Throwable ex) {
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
    printPlainText(message, null, getLogger()::info);
  }

  /**
   * Send msg as INFO log and detail in JSON format.
   *
   * @param message the debug message.
   * @param detail The additional object to be logged.
   */
  public static void info(final String message, final Object detail) {
    printPlainText(message, detail, getLogger()::info);
  }

  /**
   * Send msg as INFO log and detail as a Java bean. Ignore properties that are annotated with @PII.
   *
   * @param msg the debug message.
   * @param detail The additional object to be logged.
   */
  public static void infoB(final String msg, final Object detail) {
    printBeanFormat(msg, detail, getLogger()::info);
  }

  /**
   * Send msg and configuration object as INFO log. Ignore methods that are annotated with @Secret.
   *
   * @param msg the message.
   * @param config the configuration to be logged.
   */
  public static void infoConfig(final String msg, final Object config, final Class<?> configClazz) {
    Logger logger = getLogger();
    logger.info(msg);
    try {
      StringBuilder sb = new StringBuilder("{");
      Method[] methods = configClazz.getMethods();
      for (int i = 0; i < methods.length; i++) {
        Method method = methods[i];
        if (!method.isAnnotationPresent(Secret.class)) {
          Object result = method.invoke(config);
          sb.append(String.format("'%s': '%s'", method.getName(), result));
          if (i != methods.length - 1) {
            sb.append(",");
          }
        }
      }
      sb.append("}");
      logger.info(sb.toString());
    } catch (Exception e) {
      logger.info("Unable to serialize the bean.");
    }
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
  public static String shorter(String account) {
    if (account.length() > 11) {
      return account.substring(0, 4) + "..." + account.substring(account.length() - 4);
    } else {
      return account;
    }
  }

  /**
   * Send msg as TRACE log and detail as a Java bean. Ignore properties that are annotated
   * with @PII.
   *
   * @param msg the debug message.
   * @param detail The additional object to be logged.
   */
  public static void traceB(final String msg, final Object detail) {
    Logger logger = getLogger();
    printBeanFormat(msg, detail, logger::trace);
  }

  /**
   * Send TRACE log.
   *
   * @param message the trace message.
   */
  public static void trace(final String message) {
    printPlainText(message, null, getLogger()::trace);
  }

  /**
   * Send the msg as TRACE log and detail as JSON.
   *
   * @param message the trace message.
   * @param detail The additional object to be logged.
   */
  public static void trace(final String message, Object detail) {
    printPlainText(message, detail, getLogger()::trace);
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
  public static void warn(String message) {
    printPlainText(message, null, getLogger()::warn);
  }

  /**
   * Send the msg as WARN log and detail as JSON.
   *
   * @param message the warn message.
   * @param detail The additional object to be logged.
   */
  public static void warn(final String message, Object detail) {
    printPlainText(message, detail, getLogger()::warn);
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

  static void printPlainText(
      final String message, final Object detail, final Consumer<String> output) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    if (message != null) {
      pw.println(message);
    }
    if (detail != null) {
      pw.println(gson.toJson(detail));
    }
    output.accept(sw.toString());
  }

  static void printBeanFormat(final String message, final Object detail, Consumer<String> output) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println(message);
    BeanInfo beanInfo;
    try {
      StringBuilder sb = new StringBuilder("{\n");
      beanInfo = Introspector.getBeanInfo(detail.getClass());
      PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
      for (int i = 0; i < pds.length; i++) {
        PropertyDescriptor pd = pds[i];
        try {
          Field field = detail.getClass().getDeclaredField(pd.getName());
          if (field.isAnnotationPresent(PII.class)) {
            continue;
          }
        } catch (NoSuchFieldException ex) {
          // do nothing. proceed to check method
        }

        if (pd.getReadMethod().isAnnotationPresent(PII.class)) {
          continue;
        }

        if (pd.getName().equals("class")) {
          continue;
        }
        Object value = pd.getReadMethod().invoke(detail);
        pw.print(String.format("'%s': '%s'", pd.getName(), value));
        if (i != pds.length - 1) {
          pw.print(",");
        }
      }
      sb.append("}");
      output.accept(sb.toString());
    } catch (Exception e) {
      output.accept("Unable to serialize the bean.");
    }
  }
}
