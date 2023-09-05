package org.stellar.anchor.reference;

import com.google.gson.Gson;
import javax.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.filter.ApiKeyFilter;
import org.stellar.anchor.filter.NoneFilter;
import org.stellar.anchor.reference.config.*;
import org.stellar.anchor.reference.event.AbstractEventListener;
import org.stellar.anchor.reference.event.AnchorEventProcessor;
import org.stellar.anchor.reference.event.KafkaListener;
import org.stellar.anchor.reference.event.SqsListener;
import org.stellar.anchor.reference.filter.CallbackAuthJwtFilter;
import org.stellar.anchor.util.GsonUtils;

@Configuration
public class AnchorReferenceConfig {
  @Bean
  public Gson gson() {
    return GsonUtils.getInstance();
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

  /**
   * Register platform-to-anchor token filter.
   *
   * @return Spring Filter Registration Bean
   */
  @Bean
  public FilterRegistrationBean<Filter> platformToAnchorTokenFilter(
      IntegrationAuthSettings integrationAuthSettings) {
    Filter platformToAnchorFilter;
    String authSecret = integrationAuthSettings.getPlatformToAnchorSecret();
    switch (integrationAuthSettings.getAuthType()) {
      case JWT:
        JwtService jwtService = new JwtService(null, null, null, authSecret, null);
        platformToAnchorFilter = new CallbackAuthJwtFilter(jwtService);
        break;

      case API_KEY:
        platformToAnchorFilter = new ApiKeyFilter(authSecret);
        break;

      default:
        platformToAnchorFilter = new NoneFilter();
        break;
    }

    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(platformToAnchorFilter);
    return registrationBean;
  }

  @Bean
  AuthHelper authHelper(IntegrationAuthSettings integrationAuthSettings) {
    String authSecret = integrationAuthSettings.getAnchorToPlatformSecret();
    switch (integrationAuthSettings.getAuthType()) {
      case JWT:
        return AuthHelper.forJwtToken(
            new JwtService(null, null, null, null, authSecret),
            integrationAuthSettings.getExpirationMilliseconds());
      case API_KEY:
        return AuthHelper.forApiKey(authSecret);

      default:
        return AuthHelper.forNone();
    }
  }
}
