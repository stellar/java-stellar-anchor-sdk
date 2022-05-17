package org.stellar.anchor.util;

import com.google.gson.Gson;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logging utility functions. */
public class Log {
  static final Gson gson = GsonUtils.builder().setPrettyPrinting().create();

  /**
   * Send msg as INFO log.
   *
   * @param msg the debug message.
   */
  public static void info(final String msg) {
    Logger logger = getLogger();
    logger.info(msg);
  }

  /**
   * Send msg as INFO log and detail in JSON format.
   *
   * @param msg the debug message.
   * @param detail The additional object to be logged.
   */
  public static void info(final String msg, final Object detail) {
    Logger logger = getLogger();
    logger.info(msg);
    logger.info(gson.toJson(detail));
  }

  /**
   * Send msg as INFO log and detail as a Java bean.
   *
   * @param msg the debug message.
   * @param detail The additional object to be logged.
   */
  public static void infoB(final String msg, final Object detail) {
    Logger logger = getLogger();
    logger.info(msg);
    BeanInfo beanInfo;
    try {
      StringBuilder sb = new StringBuilder("{\n");
      beanInfo = Introspector.getBeanInfo(detail.getClass());
      PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
      for (PropertyDescriptor pd : pds) {
        Object value = pd.getReadMethod().invoke(detail);
        sb.append(String.format("'%s': '%s'\n", pd.getName(), value));
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
   * Send debug log.
   *
   * @param msg the debug message.
   */
  public static void debug(final String msg) {
    Logger logger = getLogger();
    logger.debug(msg);
  }

  /**
   * Send the msg as DEBUG log and detail as JSON.
   *
   * @param msg the debug message.
   * @param detail The additional object to be logged.
   */
  public static void debug(final String msg, Object detail) {
    Logger logger = getLogger();
    logger.debug(msg);
    if (detail != null) {
      logger.debug(gson.toJson(detail));
    }
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
   * Send message to WARN log.
   *
   * @param msg The message
   */
  public static void warn(String msg) {
    Logger logger = getLogger();
    logger.warn(msg);
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
   * Send message to ERROR log.
   *
   * @param msg The message
   */
  public static void error(String msg) {
    Logger logger = getLogger();
    logger.error(msg);
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
   * Send exception ERROR log.
   *
   * @param ex The exception.
   */
  public static void errorEx(final Throwable ex) {
    Logger logger = getLogger();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ex.printStackTrace(pw);
    logger.error(sw.toString());
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

  static Logger getLogger() {
    String cls = Thread.currentThread().getStackTrace()[3].getClassName();
    return LoggerFactory.getLogger(cls);
  }
}
