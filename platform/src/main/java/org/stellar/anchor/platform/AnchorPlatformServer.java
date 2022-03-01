package org.stellar.anchor.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.stellar.anchor.platform.configurator.DataAccessConfigurator;
import org.stellar.anchor.platform.configurator.PlatformAppConfigurator;
import org.stellar.anchor.platform.configurator.PropertiesReader;
import org.stellar.anchor.platform.configurator.SpringFrameworkConfigurator;

@SpringBootApplication
@EnableConfigurationProperties
public class AnchorPlatformServer implements WebMvcConfigurer {
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(AnchorPlatformServer.class);

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
