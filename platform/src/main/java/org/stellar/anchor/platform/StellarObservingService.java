package org.stellar.anchor.platform;

import static org.springframework.boot.Banner.Mode.OFF;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.stellar.anchor.platform.configurator.ConfigEnvironment;
import org.stellar.anchor.platform.configurator.ConfigManager;

@Profile("stellar-observer")
@SpringBootApplication
@EnableJpaRepositories(basePackages = {"org.stellar.anchor.platform.data"})
@EntityScan(basePackages = {"org.stellar.anchor.platform.data"})
@EnableConfigurationProperties
public class StellarObservingService implements WebMvcConfigurer {

  public static ConfigurableApplicationContext start(Map<String, Object> environment) {
    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(StellarObservingService.class)
            .bannerMode(OFF)
            .web(WebApplicationType.NONE)
            .properties(
                // TODO: update when the ticket
                // https://github.com/stellar/java-stellar-anchor-sdk/issues/297 is completed.
                "spring.mvc.converters.preferred-json-mapper=gson",
                // this allows a developer to use a .env file for local development
                "spring.config.import=optional:classpath:example.env[.properties]",
                "spring.profiles.active=stellar-observer");

    if (environment != null) {
      for (String name : environment.keySet()) {
        System.setProperty(name, String.valueOf(environment.get(name)));
      }
      ConfigEnvironment.reset();
    }

    SpringApplication springApplication = builder.build();

    springApplication.addInitializers(ConfigManager.getInstance());

    return springApplication.run();
  }

  public static ConfigurableApplicationContext start() {
    return start(null);
  }
}
