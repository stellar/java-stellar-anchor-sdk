package org.stellar.anchor.platform;

import static org.springframework.boot.Banner.Mode.OFF;

import com.google.gson.Gson;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.stellar.anchor.config.MetricConfig;
import org.stellar.anchor.platform.configurator.DataAccessConfigurator;
import org.stellar.anchor.platform.configurator.PlatformAppConfigurator;
import org.stellar.anchor.platform.configurator.PropertiesReader;
import org.stellar.anchor.platform.configurator.SpringFrameworkConfigurator;
import org.stellar.anchor.platform.data.JdbcSep31TransactionRepo;
import org.stellar.anchor.platform.data.JdbcSep31TransactionStore;
import org.stellar.anchor.platform.service.MetricEmitterService;
import org.stellar.anchor.util.GsonUtils;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"org.stellar.anchor.platform.data"})
@EntityScan(basePackages = {"org.stellar.anchor.platform.data"})
@EnableConfigurationProperties
public class AnchorPlatformServer implements WebMvcConfigurer {

  public static ConfigurableApplicationContext start(
      int port, String contextPath, Map<String, Object> environment) {
    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(AnchorPlatformServer.class)
            .bannerMode(OFF)
            .properties(
                // TODO: move these configs to the config file when this is fixed -
                //  https://github.com/stellar/java-stellar-anchor-sdk/issues/297
                "spring.mvc.converters.preferred-json-mapper=gson",
                "spring.config.import=optional:classpath:example.env[.properties]",
                "management.endpoints.web.exposure.include=health,info,prometheus",
                "management.server.port=8082",
                // this allows a developer to use a .env file for local development
                "spring.config.import=optional:classpath:example.env[.properties]",
                String.format("server.port=%d", port),
                String.format("server.contextPath=%s", contextPath));
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

  @Bean
  public Gson gson() {
    return GsonUtils.builder().create();
  }

  public static void start(int port, String contextPath) {
    start(port, contextPath, null);
  }

  @Bean
  public MetricEmitterService metricService(MetricConfig metricConfig, JdbcSep31TransactionRepo sep31TransactionRepo) {
    return new MetricEmitterService(metricConfig, sep31TransactionRepo);
  }
}
