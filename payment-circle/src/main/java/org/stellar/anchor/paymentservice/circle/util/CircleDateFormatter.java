package org.stellar.anchor.paymentservice.circle.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CircleDateFormatter {
  public static final SimpleDateFormat dateFormatter = _dateFormatter();

  private static SimpleDateFormat _dateFormatter() {
    SimpleDateFormat circleDateFormatter =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
    circleDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    return circleDateFormatter;
  }

  public static Date stringToDate(String dateStr) throws ParseException {
    return dateFormatter.parse(dateStr);
  }

  public static String dateToString(Date date) {
    return dateFormatter.format(date);
  }
}
