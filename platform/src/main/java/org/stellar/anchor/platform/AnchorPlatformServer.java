package org.stellar.anchor.platform;

import static org.springframework.boot.Banner.Mode.OFF;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.stellar.anchor.platform.configurator.DataAccessConfigurator;
import org.stellar.anchor.platform.configurator.PlatformAppConfigurator;
import org.stellar.anchor.platform.configurator.PropertiesReader;
import org.stellar.anchor.platform.configurator.SpringFrameworkConfigurator;

@Profile("default")
@SpringBootApplication
@EnableJpaRepositories(basePackages = {"org.stellar.anchor.platform.data"})
@EntityScan(basePackages = {"org.stellar.anchor.platform.data"})
@EnableConfigurationProperties
public class AnchorPlatformServer implements WebMvcConfigurer {

  public static ConfigurableApplicationContext start(
      int port, String contextPath, Map<String, Object> environment, boolean disableMetrics) {
    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(AnchorPlatformServer.class)
            .bannerMode(OFF)
            .properties(
                // TODO: move these configs to the config file when this is fixed and get rid of
                //       disableMetrics param -
                //  https://github.com/stellar/java-stellar-anchor-sdk/issues/297
                "spring.mvc.converters.preferred-json-mapper=gson",
                // this allows a developer to use a .env file for local development
                "spring.config.import=optional:classpath:example.env[.properties]",
                String.format("server.port=%d", port),
                String.format("server.contextPath=%s", contextPath));

    if (!disableMetrics) {
      builder.properties(
          "management.endpoints.web.exposure.include=health,info,prometheus",
          "management.server.port=8082");
    }
    if (environment != null) {
      builder.properties(environment);
    }

    SpringApplication springApplication = builder.build();

    // Reads the configuration from sources, such as yaml
    springApplication.addInitializers(new PropertiesReader());
    // Configure SEPs
    springApplication.addInitializers(new PlatformAppConfigurator());
    // Configure databases
    springApplication.addInitializers(new DataAccessConfigurator());
    // Configure spring framework
    springApplication.addInitializers(new SpringFrameworkConfigurator());

    return springApplication.run();
  }

  public static ConfigurableApplicationContext start(int port, String contextPath) {
    return start(port, contextPath, null, false);
  }
}
