package org.stellar.anchor.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
  /**
   * From epoch seconds to UTC ISO8601 string.
   *
   * @param epochSeconds the epoch seconds
   * @return The string in ISO8601 format.
   */
  public static String toISO8601UTC(long epochSeconds) {
    ZonedDateTime zdt =
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    return zdt.format(DateTimeFormatter.ISO_INSTANT);
  }

  /**
   * From ISO 8601 string to epoch seconds.
   *
   * @param str The string in ISO8601 format.
   * @return the epoch seconds
   */
  public static long fromISO8601UTC(String str) {
    ZonedDateTime dt = ZonedDateTime.parse(str);
    return dt.toEpochSecond();
  }
}
