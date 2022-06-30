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
import org.stellar.anchor.platform.configurator.DataAccessConfigurator;
import org.stellar.anchor.platform.configurator.PlatformAppConfigurator;
import org.stellar.anchor.platform.configurator.PropertiesReader;
import org.stellar.anchor.platform.configurator.SpringFrameworkConfigurator;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"org.stellar.anchor.platform.data"})
@EntityScan(basePackages = {"org.stellar.anchor.platform.data"})
@EnableConfigurationProperties
public class AnchorPlatformServer implements WebMvcConfigurer {
  
  public static ConfigurableApplicationContext start(
      int port, String contextPath, Map<String, Object> environment) {
        Log.debug("REECEDEBUG contextPath = '%s", contextPath);
        Log.debug("REECEDEBUG ENVIRONMENT = '%s", environment.toString());
    SpringApplicationBuilder builder =
        new SpringApplicationBuilder(AnchorPlatformServer.class)
            .bannerMode(OFF)
            .properties(
                "spring.mvc.converters.preferred-json-mapper=gson",
                // this allows a developer to use a .env file for local development
                "spring.config.import=optional:classpath:example.env[.properties]",
                String.format("server.port=%d", port),
                String.format("server.contextPath=%s", contextPath));
    
                if (environment != null) {
      Log.debug("REECEDEBUG ENVIRONMENT = '%s",environment.toString());
      builder.properties(environment);
    } else {
      Log.debug("REECEDEBUG ENVIRONMENT = null");
    }

    SpringApplication springApplication = builder.build();
    //Log.debug("springAppliclation = '%s", springApplication.toString());
    
    // Reads the configuration from sources, such as yaml
    springApplication.addInitializers(new PropertiesReader());
    // Configure SEPs
    springApplication.addInitializers(new PlatformAppConfigurator());
    // Configure databases
    springApplication.addInitializers(new DataAccessConfigurator());
    // Configure spring framework
    springApplication.addInitializers(new SpringFrameworkConfigurator());
    Log.debug("PropertySources = '%s", springApplication.getAllSources().toString());
    return springApplication.run();
  }

  @Bean
  public Gson gson() {
    return GsonUtils.builder().create();
  }

  public static void start(int port, String contextPath) {
    start(port, contextPath, null);
  }
}
