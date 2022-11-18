package org.stellar.anchor.util;

import org.springframework.validation.Errors;

public class UrlValidationUtil {

  public static void rejectIfMalformed(String url, String fieldName, Errors errors) {
    if (!NetUtil.isUrlValid(url)) {
      errors.rejectValue(
          fieldName,
          String.format("invalidUrl-%s", fieldName),
          String.format("%s is not in valid format", fieldName));
    }
  }
}
