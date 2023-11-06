package org.stellar.anchor.client;

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
import org.stellar.anchor.client.configurator.SecretManager;
import org.stellar.anchor.client.configurator.SepConfigManager;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"org.stellar.anchor.client.data"})
@EntityScan(basePackages = {"org.stellar.anchor.platform.data"})
@ComponentScan(
    basePackages = {
      "org.stellar.anchor.client.controller.sep",
      "org.stellar.anchor.client.component.sep",
      "org.stellar.anchor.client.component.share"
    })
@EnableConfigurationProperties
public class SepServer extends AbstractPlatformServer implements WebMvcConfigurer {
  private ConfigurableApplicationContext ctx;

  public ConfigurableApplicationContext start(Map<String, String> environment) {
    buildEnvironment(environment);

    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(SepServer.class).bannerMode(OFF);
    SpringApplication springApplication = builder.build();
    info("Adding secret manager as initializers...");
    springApplication.addInitializers(SecretManager.getInstance());
    info("Adding sep config manager as initializers...");
    springApplication.addInitializers(SepConfigManager.getInstance());

    return ctx = springApplication.run();
  }
}
