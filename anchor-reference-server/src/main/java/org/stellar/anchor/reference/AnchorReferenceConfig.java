package org.stellar.anchor.reference;

import com.google.gson.Gson;
import javax.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.filter.PlatformToAnchorTokenFilter;
import org.stellar.anchor.reference.config.*;
import org.stellar.anchor.reference.event.AbstractEventListener;
import org.stellar.anchor.reference.event.AnchorEventProcessor;
import org.stellar.anchor.reference.event.KafkaListener;
import org.stellar.anchor.reference.event.SqsListener;
import org.stellar.anchor.util.GsonUtils;

@Configuration
public class AnchorReferenceConfig {
  @Bean
  public Gson gson() {
    return GsonUtils.builder().create();
  }

  @Bean(name = "eventListener")
  public AbstractEventListener eventListener(
      EventSettings eventSettings,
      AnchorEventProcessor anchorEventProcessor,
      KafkaListenerSettings kafkaListenerSettings,
      SqsListenerSettings sqsListenerSettings) {
    switch (eventSettings.getListenerType()) {
      case "kafka":
        return new KafkaListener(kafkaListenerSettings, anchorEventProcessor);
      case "sqs":
        return new SqsListener(sqsListenerSettings, anchorEventProcessor);
      case "amqp":
      default:
        throw new RuntimeException(
            String.format("Invalid event listener: %s", eventSettings.getListenerType()));
    }
  }

  @Bean
  public Filter platformToAnchorFilter(IntegrationAuthSettings integrationAuthSettings) {
    String jwtSecret = integrationAuthSettings.getPlatformToAnchorJwtSecret();
    JwtService jwtService = new JwtService(jwtSecret);
    return new PlatformToAnchorTokenFilter(jwtService);
  }

  /**
   * Register platform-to-anchor token filter.
   *
   * @return Spring Filter Registration Bean
   */
  @Bean
  public FilterRegistrationBean<Filter> platformToAnchorTokenFilter(Filter platformToAnchorFilter) {
    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(platformToAnchorFilter);
    registrationBean.addUrlPatterns("/*");
    registrationBean.addUrlPatterns("");
    return registrationBean;
  }

  @Bean
  AuthHelper authHelper(AppSettings appSettings, IntegrationAuthSettings integrationAuthSettings) {
    return new AuthHelper(
        new JwtService(integrationAuthSettings.getAnchorToPlatformJwtSecret()),
        integrationAuthSettings.getExpirationMilliseconds(),
        appSettings.getHostUrl());
  }
}
