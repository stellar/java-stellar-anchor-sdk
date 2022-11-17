package org.stellar.anchor.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.springframework.validation.Errors;

public class UrlValidationUtil {
  static UrlConnectionStatus validateUrl(String urlString, boolean testConnection) {
    try {
      URL url = new URL(urlString);
      if (testConnection) {
        URLConnection conn = url.openConnection();
        conn.connect();
      }
    } catch (MalformedURLException e) {
      return UrlConnectionStatus.MALFORMED;
    } catch (IOException e) {
      return UrlConnectionStatus.UNREACHABLE;
    }
    return UrlConnectionStatus.VALID;
  }

  public static void rejectIfMalformed(String url, String fieldName, Errors errors) {
    if (!NetUtil.isUrlValid(url)) {
      errors.rejectValue(
          fieldName,
          String.format("invalidUrl-%s", fieldName),
          String.format("%s is not in valid format", fieldName));
    }
  }
}
