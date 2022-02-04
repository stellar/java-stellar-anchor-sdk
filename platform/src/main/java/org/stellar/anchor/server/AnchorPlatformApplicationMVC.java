package org.stellar.anchor.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackages = {"org.stellar.anchor.server.config"})
public class AnchorPlatformApplicationMVC implements WebMvcConfigurer {
  public static void main(String[] args) {
    new SpringApplication(AnchorPlatformApplicationMVC.class).run();
  }
}
