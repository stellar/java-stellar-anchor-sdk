package org.stellar.anchor.reference;

import static org.springframework.boot.Banner.Mode.OFF;

import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableConfigurationProperties
public class AnchorReferenceServer implements WebMvcConfigurer {
  static final String REFERENCE_SERVER_CONFIG_ENV = "REFERENCE_SERVER_CONFIG_ENV";
  static final String REFERENCE_SERVER_PORT = "REFERENCE_SERVER_PORT";
  static final String REFERENCE_SERVER_CONTEXT_PATH = "REFERENCE_SERVER_CONTEXT_PATH";

  public static void start(int port, String contextPath) {
    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(AnchorReferenceServer.class)
            .bannerMode(OFF)
            .properties(
                String.format("server.port=%d", port),
                String.format("server.contextPath=%s", contextPath),
                "spring.liquibase.enabled=false");

    SpringApplication app = builder.build();
    app.addInitializers(new PropertySourceInitializer());
    app.run();
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
          applicationContext.getEnvironment().getPropertySources().addFirst(source);
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
        Integer.parseInt(getProperty(REFERENCE_SERVER_PORT, "8081")),
        getProperty(REFERENCE_SERVER_CONTEXT_PATH, "/"));
  }
}
