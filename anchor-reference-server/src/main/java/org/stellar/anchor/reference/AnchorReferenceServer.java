package org.stellar.anchor.reference;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.boot.Banner.Mode.OFF;

@SpringBootApplication
@EnableConfigurationProperties
@PropertySource("/anchor-reference-server.yaml")
public class AnchorReferenceServer implements WebMvcConfigurer {
  public static void start(int port, String contextPath) {
    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(AnchorReferenceServer.class)
            .bannerMode(OFF)
            .properties(
                String.format("server.port=%d", port),
                String.format("server.contextPath=%s", contextPath));

    builder.build().run();
  }

  public static void main(String[] args) {
    start(8081, "/");
  }
}
