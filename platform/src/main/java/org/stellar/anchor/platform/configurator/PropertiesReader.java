package org.stellar.anchor.platform.configurator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.stellar.anchor.util.Log;
public class PropertiesReader extends AbstractConfigurator
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @SneakyThrows
  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    // Load default values
    loadConfigYaml(new ClassPathResource("anchor-config-defaults.yaml"));

    // If stellar.anchor.config is specified in Spring application properties, use it.
    // This is mainly for the purpose of integration test where we may inject the configuration yaml
    // file from the test resource.
    String yamlLocation = applicationContext.getEnvironment().getProperty("stellar.anchor.config");
    if (yamlLocation != null) {
      loadConfigYaml(applicationContext, yamlLocation);
      return;
    }

    // Read from Java VM OPTS
    yamlLocation = getFromSystemProperty();
    Log.debugF("REECE yamlLocation = {}", yamlLocation);
    if (yamlLocation != null) {
      loadConfigYaml(applicationContext, yamlLocation);
      return;
    }

    // Read from $USER_HOME/.anchor/anchor-config.yaml
    File yamlFile = getFromUserFolder();
    Log.debugF("REECE yamlFile = {}", yamlFile.toString());
    if (yamlFile.exists()) {
      Log.debugF("REECE yamlFile = {} exists loading it", yamlFile.toString());
      loadConfigYaml(new FileSystemResource(yamlFile));
      return;
    }

    // Read from the file specified by STELLAR_ANCHOR_CONFIG environment variable.
    yamlLocation = getFromSystemEnv();
    Log.debugF("REECE DEBUG yamlLocation={}",yamlLocation);
    if (yamlLocation != null) {
      Log.debugF("REECEDEBUG yamlLocation exists in Env = {}",yamlLocation);
      
      loadConfigYaml(applicationContext, yamlLocation);
      return;
    } else
    {
      Log.debugF("REECEDEBUG STELLAR_ANCHOR_CONFIG = null yamlLocation did not exist");
    }

    throw new IllegalArgumentException("Unable to load anchor platform configuration file.");
  }

  String getFromSystemEnv() {
    Log.debugF("REECEDEBUG STELLAR_ANCHOR_CONFIG = {}",System.getenv("STELLAR_ANCHOR_CONFIG"));
    String yamLocation = System.getenv().getOrDefault("STELLAR_ANCHOR_CONFIG", "classpath:/anchor-config-defaults.yaml");
    return yamLocation;
  }

  String getFromSystemProperty() {
    return System.getProperty("stellar.anchor.config");
  }

  File getFromUserFolder() {
    return new File(System.getProperty("user.home"), ".anchor/anchor-config.yaml");
  }

  void loadConfigYaml(ApplicationContext applicationContext, String location) throws IOException {
    Resource resource = applicationContext.getResource(location);
    if (!resource.exists()) {
      throw new IOException("Resource not found");
    }
    loadConfigYaml(resource);
  }

  void loadConfigYaml(Resource resource) throws IOException {
    Properties flattenedProperty = getFlatProperties();

    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    List<PropertySource<?>> sources = loader.load("yaml", resource);

    for (PropertySource<?> source : sources) {
      MapPropertySource mapPropertySource = (MapPropertySource) source;
      for (Map.Entry<String, Object> entry : mapPropertySource.getSource().entrySet()) {
        flattenedProperty.put(entry.getKey(), entry.getValue().toString());
      }
    }
  }
}
