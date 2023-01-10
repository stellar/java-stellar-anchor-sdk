package org.stellar.anchor.platform;

import static org.springframework.boot.Banner.Mode.OFF;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.stellar.anchor.platform.configurator.ConfigEnvironment;
import org.stellar.anchor.platform.configurator.ObserverConfigManager;
import org.stellar.anchor.platform.configurator.SecretManager;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"org.stellar.anchor.platform.data"})
@EntityScan(basePackages = {"org.stellar.anchor.platform.data"})
@ComponentScan(
    basePackages = {
      "org.stellar.anchor.platform.controller.observer",
      "org.stellar.anchor.platform.component.observer",
      "org.stellar.anchor.platform.component.share"
    })
@EnableConfigurationProperties
public class StellarObservingServer implements WebMvcConfigurer {
  public static ConfigurableApplicationContext start(Map<String, Object> environment) {
    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(StellarObservingServer.class).bannerMode(OFF);

    if (environment != null) {
      for (String name : environment.keySet()) {
        System.setProperty(name, String.valueOf(environment.get(name)));
      }
      ConfigEnvironment.rebuild();
    }

    SpringApplication springApplication = builder.build();

    springApplication.addInitializers(SecretManager.getInstance());
    springApplication.addInitializers(ObserverConfigManager.getInstance());

    return springApplication.run();
  }
}
