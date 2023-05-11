package org.stellar.anchor.platform.component.share;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.horizon.Horizon;

@Configuration
public class HorizonBeans {

  @Bean
  public Horizon horizon(AppConfig appConfig) {
    return new Horizon(appConfig);
  }
}
