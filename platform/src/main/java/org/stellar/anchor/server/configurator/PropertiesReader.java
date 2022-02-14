package org.stellar.anchor.server.configurator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class PropertiesReader extends AbstractConfigurator
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @SneakyThrows
  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    loadConfigYaml("default", new ClassPathResource("default-config.yaml"));

    // Read from $USER_HOME/.anchor/anchor-config.yaml
    File yamlFile = new File(System.getProperty("user.home"), "./anchor/anchor-config.yaml");
    if (yamlFile.exists()) {
      loadConfigYaml("yaml", new FileSystemResource(yamlFile));
      return;
    }

    // Read from the file specified by STELLAR_ANCHOR_CONFIG environment variable.
    String yamlFilePath = System.getenv().get("STELLAR_ANCHOR_CONFIG");
    if (yamlFilePath != null) {
      loadConfigYaml("yaml", new FileSystemResource(yamlFilePath));
      return;
    }

    // Read from Java VM
    yamlFilePath = System.getProperty("stellar.anchor.config");
    if (yamlFilePath != null) {
      loadConfigYaml("yaml", new FileSystemResource(yamlFilePath));
    }
  }

  private void loadConfigYaml(String name, Resource resource) throws IOException {
    Properties flattenedProperty = getFlatProperties();

    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    List<PropertySource<?>> sources = loader.load(name, resource);

    for (PropertySource<?> source : sources) {
      MapPropertySource mapPropertySource = (MapPropertySource) source;
      for (Map.Entry<String, Object> entry : mapPropertySource.getSource().entrySet()) {
        flattenedProperty.put(entry.getKey(), entry.getValue().toString());
      }
    }
  }
}
