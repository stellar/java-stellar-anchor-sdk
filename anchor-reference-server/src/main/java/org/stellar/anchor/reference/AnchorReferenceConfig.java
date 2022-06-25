package org.stellar.anchor.reference;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.reference.config.EventSettings;
import org.stellar.anchor.reference.config.IntegrationAuthSettings;
import org.stellar.anchor.reference.config.KafkaListenerSettings;
import org.stellar.anchor.reference.config.SqsListenerSettings;
import org.stellar.anchor.reference.event.AbstractEventListener;
import org.stellar.anchor.reference.event.AnchorEventProcessor;
import org.stellar.anchor.reference.event.KafkaListener;
import org.stellar.anchor.reference.event.SqsListener;
import org.stellar.anchor.reference.filter.PlatformToAnchorTokenFilter;
import org.stellar.anchor.sep10.JwtService;
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

  /**
   * Register platform-to-anchor token filter.
   *
   * @return Spring Filter Registration Bean
   */
  @Bean
  public FilterRegistrationBean<PlatformToAnchorTokenFilter> sep10TokenFilter(
      @Autowired IntegrationAuthSettings integrationAuthSettings) {
    JwtService platformToAnchorJwtService =
        new JwtService(integrationAuthSettings.getPlatformToAnchorJwtSecret());
    FilterRegistrationBean<PlatformToAnchorTokenFilter> registrationBean =
        new FilterRegistrationBean<>();
    registrationBean.setFilter(new PlatformToAnchorTokenFilter(platformToAnchorJwtService));
    registrationBean.addUrlPatterns("/*");
    registrationBean.addUrlPatterns("/");
    return registrationBean;
  }
}
