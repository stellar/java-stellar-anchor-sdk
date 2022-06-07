package org.stellar.anchor.util;

import static org.stellar.anchor.util.Log.errorF;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.stellar.anchor.api.exception.SepNotFoundException;

public class FileUtil {
  public static String getResourceFileAsString(String fileName)
      throws IOException, SepNotFoundException {
    ClassLoader classLoader = FileUtil.class.getClassLoader();
    try (InputStream is = classLoader.getResourceAsStream(fileName)) {
      if (is == null) {
        errorF("{} was not found in class path.", fileName);
        throw new SepNotFoundException(String.format("%s was not found in classpath.", fileName));
      }
      try (InputStreamReader isr = new InputStreamReader(is);
          BufferedReader reader = new BufferedReader(isr)) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    }
  }
}
