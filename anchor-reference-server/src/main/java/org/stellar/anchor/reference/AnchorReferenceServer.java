package org.stellar.anchor.reference;

import static org.springframework.boot.Banner.Mode.OFF;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableConfigurationProperties
public class AnchorReferenceServer implements WebMvcConfigurer {
  static final String REFERENCE_SERVER_CONFIG_ENV = "REFERENCE_SERVER_CONFIG_ENV";
  static final String REFERENCE_SERVER_PORT = "REFERENCE_SERVER_PORT";
  static final String REFERENCE_SERVER_CONTEXT_PATH = "REFERENCE_SERVER_CONTEXT_PATH";

  static ConfigurableApplicationContext ctx;

  public static ConfigurableApplicationContext start(
      Map<String, String> envMap, int port, String contextPath) {
    if (envMap == null) {
      envMap = new HashMap<>();
    }
    envMap.put("server.port", String.valueOf(port));
    envMap.put("server.contextPath", String.valueOf(contextPath));

    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(AnchorReferenceServer.class).bannerMode(OFF);

    SpringApplication app = builder.build();
    app.addInitializers(new EnvironmentPropertySourceOverrider(envMap));
    app.addInitializers(new PropertySourceInitializer());
    return ctx = app.run();
  }

  public static void stop() {
    if (ctx != null) {
      SpringApplication.exit(ctx);
    }
  }

  public static class EnvironmentPropertySourceOverrider
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private final Properties environmentProperties;

    public EnvironmentPropertySourceOverrider(Map<String, String> envMap) {
      super();
      environmentProperties = new Properties();
      environmentProperties.putAll(envMap);
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      applicationContext
          .getEnvironment()
          .getPropertySources()
          .addFirst(new PropertiesPropertySource("envProps", environmentProperties));
    }
  }

  public static class PropertySourceInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
      String referenceServerConfig =
          AnchorReferenceServer.getProperty(
              REFERENCE_SERVER_CONFIG_ENV, "classpath:/anchor-reference-server.yaml");

      Resource resource = applicationContext.getResource(referenceServerConfig);
      try {
        List<org.springframework.core.env.PropertySource<?>> sources =
            new YamlPropertySourceLoader().load("yaml", resource);
        for (org.springframework.core.env.PropertySource<?> source : sources) {
          applicationContext.getEnvironment().getPropertySources().addLast(source);
        }
      } catch (IOException e) {
        throw new RuntimeException(
            String.format("Error reading configuration from %s", referenceServerConfig));
      }
    }
  }

  static String getProperty(String name, String defaultValue) {
    String value = System.getenv().get(name);
    if (value == null) {
      value = System.getProperty(name);
    }
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  public static void main(String[] args) {
    start(
        null,
        Integer.parseInt(getProperty(REFERENCE_SERVER_PORT, "8081")),
        getProperty(REFERENCE_SERVER_CONTEXT_PATH, "/"));
  }
}
