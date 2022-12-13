package org.stellar.anchor.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
  /**
   * From epoch seconds to UTC ISO8601 string.
   *
   * @param instant the Instant object
   * @return The string in ISO8601 format.
   */
  public static String toISO8601UTC(Instant instant) {
    ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
    return zdt.format(DateTimeFormatter.ISO_INSTANT);
  }

  /**
   * From ISO 8601 string to epoch seconds.
   *
   * @param str The string in ISO8601 format.
   * @return the Instant
   */
  public static Instant fromISO8601UTC(String str) {
    ZonedDateTime dt = ZonedDateTime.parse(str);
    return dt.toInstant();
  }
}
