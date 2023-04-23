package org.stellar.anchor.platform.component.custody;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.job.ReconcileFireblocksTransactionsJob;

@Configuration
@ConditionalOnProperty(value = "custody.fireblocks.enabled", havingValue = "true")
public class FireblocksBeans {
  @Bean
  ReconcileFireblocksTransactionsJob reconciliationJob() {
    return new ReconcileFireblocksTransactionsJob();
  }
}
