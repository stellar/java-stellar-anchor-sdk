package org.stellar.anchor.util;

import java.lang.reflect.Field;
import org.stellar.sdk.requests.SSEStream;

public class ReflectionUtil {
  public static <T> T getField(Object target, String fieldName, T defaultValue) {
    try {
      // populate executorService information
      Field field = SSEStream.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return (T) field.get(target);
    } catch (NoSuchFieldException | IllegalAccessException nsfex) {
      return defaultValue;
    }
  }
}
