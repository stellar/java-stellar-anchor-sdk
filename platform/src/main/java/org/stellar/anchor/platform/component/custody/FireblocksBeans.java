package org.stellar.anchor.platform.component.custody;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.job.FireblocksTransactionsReconciliationJob;
import org.stellar.anchor.platform.service.FireblocksEventsService;

@Configuration
@ConditionalOnProperty(value = "custody.fireblocks.enabled", havingValue = "true")
public class FireblocksBeans {
  @Bean
  FireblocksTransactionsReconciliationJob reconciliationJob() {
    return new FireblocksTransactionsReconciliationJob();
  }

  @Bean
  FireblocksEventsService fireblocksEventsService(
      @Value("${custody.fireblocks.public_key}") String publicKey) {
    return new FireblocksEventsService(publicKey);
  }
}
