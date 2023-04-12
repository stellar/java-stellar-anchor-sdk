package org.stellar.anchor.platform;

import static org.springframework.boot.Banner.Mode.OFF;
import static org.stellar.anchor.util.Log.info;

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
public class StellarObservingServer extends AbstractPlatformServer implements WebMvcConfigurer {
  public ConfigurableApplicationContext start(Map<String, String> environment) {
    buildEnvironment(environment);

    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(StellarObservingServer.class).bannerMode(OFF);
    SpringApplication springApplication = builder.build();
    info("Adding secret manager as initializers...");
    springApplication.addInitializers(SecretManager.getInstance());
    info("Adding observer config manager as initializers...");
    springApplication.addInitializers(ObserverConfigManager.getInstance());

    return ctx = springApplication.run();
  }
}
