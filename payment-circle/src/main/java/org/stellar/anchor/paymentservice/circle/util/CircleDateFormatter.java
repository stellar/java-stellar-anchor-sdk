package org.stellar.anchor.paymentservice.circle.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class CircleDateFormatter {
  public static final DateTimeFormatter dateFormatter = _dateFormatter();
  private static final ZoneId zone = ZoneId.of("UTC");

  private static DateTimeFormatter _dateFormatter() {
    ZoneId zone = ZoneId.of("UTC");
    return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        .withZone(zone);
  }

  public static Date stringToDate(String dateStr) {
    LocalDateTime localDateTime = LocalDateTime.parse(dateStr, dateFormatter);
    return Date.from(localDateTime.atZone(zone).toInstant());
  }

  public static String dateToString(Date date) {
    LocalDateTime localDateTime = date.toInstant().atZone(zone).toLocalDateTime();
    return dateFormatter.format(localDateTime);
  }
}
