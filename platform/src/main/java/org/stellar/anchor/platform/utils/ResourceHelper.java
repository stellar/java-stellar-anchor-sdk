package org.stellar.anchor.platform.utils;

import java.io.File;
import java.io.IOException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class ResourceHelper {
  static final ResourceLoader resourceLoader = new DefaultResourceLoader();

  public static Resource resource(String resource) {
    return resourceLoader.getResource(resource);
  }

  public static File findResourceFile(Resource resource) throws IOException {
    if (resource.exists()) {
      return resource.getFile();
    }
    throw new IOException("Resource not found: " + resource.getFilename());
  }
}
