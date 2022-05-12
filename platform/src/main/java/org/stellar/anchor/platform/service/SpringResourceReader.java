package org.stellar.anchor.platform.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;
import org.stellar.anchor.util.ResourceReader;

public class SpringResourceReader implements ResourceReader {
  final ResourceLoader resourceLoader = new DefaultResourceLoader();

  @Override
  public String readResourceAsString(String path) {
    Resource resource = resourceLoader.getResource(path);
    return asString(resource);
  }

  public String asString(Resource resource) {
    try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
      return FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
