package org.stellar.anchor.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.stellar.anchor.server.configurator.*;

@SpringBootApplication
@EnableConfigurationProperties
public class AnchorPlatformApplication implements WebMvcConfigurer {
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(AnchorPlatformApplication.class);

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
}
