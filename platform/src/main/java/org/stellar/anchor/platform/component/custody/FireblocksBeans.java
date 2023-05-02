package org.stellar.anchor.platform.component.custody;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.platform.config.CustodySecretConfig;
import org.stellar.anchor.platform.config.FireblocksConfig;
import org.stellar.anchor.platform.custody.service.FireblocksEventsService;
import org.stellar.anchor.platform.job.FireblocksTransactionsReconciliationJob;

@Configuration
@ConditionalOnProperty(value = "custody.type", havingValue = "fireblocks")
public class FireblocksBeans {

  @Bean
  @ConfigurationProperties(prefix = "custody.fireblocks")
  FireblocksConfig fireblocksConfig(CustodySecretConfig custodySecretConfig) {
    return new FireblocksConfig(custodySecretConfig);
  }

  @Bean
  FireblocksTransactionsReconciliationJob reconciliationJob() {
    return new FireblocksTransactionsReconciliationJob();
  }

  @Bean
  FireblocksEventsService fireblocksEventsService(FireblocksConfig fireblocksConfig)
      throws InvalidConfigException {
    return new FireblocksEventsService(fireblocksConfig);
  }
}
