package org.stellar.anchor.reference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableConfigurationProperties
public class AnchorReferenceServer implements WebMvcConfigurer {
  public static void main(String[] args) {
    SpringApplication.run(AnchorReferenceServer.class);
  }
}
