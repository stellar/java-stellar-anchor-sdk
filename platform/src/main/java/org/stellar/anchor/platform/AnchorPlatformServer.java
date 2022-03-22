package org.stellar.anchor.platform;

import static org.springframework.boot.Banner.Mode.OFF;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.stellar.anchor.platform.configurator.DataAccessConfigurator;
import org.stellar.anchor.platform.configurator.PlatformAppConfigurator;
import org.stellar.anchor.platform.configurator.PropertiesReader;
import org.stellar.anchor.platform.configurator.SpringFrameworkConfigurator;



@SpringBootApplication
@EnableJpaRepositories(
    basePackages = {"org.stellar.anchor.platform", "org.stellar.anchor.server.data"})
@EntityScan(basePackages = {"org.stellar.anchor.platform", "org.stellar.anchor.server.data"})
@EnableConfigurationProperties
@PropertySource("/anchor-platform-server.yaml")
public class AnchorPlatformServer implements WebMvcConfigurer {
  public static void main(String[] args) {
    start(8080, "/");
  }

  public static void start(int port, String contextPath, Map<String, Object> environment) {
    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(AnchorPlatformServer.class)
            .bannerMode(OFF)
            .properties(
                String.format("server.port=%d", port),
                String.format("server.contextPath=%s", contextPath));
    if (environment != null) {
      builder.properties(environment);
    }

    SpringApplication app = builder.build();

    // Reads the configuration from sources, such as yaml
    app.addInitializers(new PropertiesReader());
    // Configure SEPs
    app.addInitializers(new PlatformAppConfigurator());
    // Configure databases
    app.addInitializers(new DataAccessConfigurator());
    // Configure spring framework
    app.addInitializers(new SpringFrameworkConfigurator());

    app.run();
  }

  public static void start(int port, String contextPath) {
    start(port, contextPath, null);
  }
}
