package org.stellar.anchor.reference;

import static org.springframework.boot.Banner.Mode.OFF;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.stellar.anchor.util.GsonUtils;

import java.util.List;

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

  @Bean
  public HttpMessageConverter<?> gonMessageConverter() {
    return new GsonHttpMessageConverter(new Gson());
  }

  @Bean
  public Gson gson() {
    return GsonUtils.builder().create();
  }

  public static void main(String[] args) {
    start(8081, "/");
  }
}
