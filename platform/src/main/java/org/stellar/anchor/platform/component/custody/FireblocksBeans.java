package org.stellar.anchor.platform.component.custody;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.platform.config.FireblocksConfig;
import org.stellar.anchor.platform.custody.PaymentService;
import org.stellar.anchor.platform.custody.fireblocks.FireblocksClient;
import org.stellar.anchor.platform.custody.fireblocks.FireblocksPaymentService;
import org.stellar.anchor.platform.job.FireblocksTransactionsReconciliationJob;
import org.stellar.anchor.platform.service.FireblocksEventsService;

@Configuration
@ConditionalOnProperty(value = "custody.type", havingValue = "fireblocks")
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

  @Bean
  @Qualifier("fireblocksHttpClient")
  OkHttpClient fireblocksHttpClient() {
    return new Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build();
  }

  @Bean
  FireblocksClient fireblocksClient(
      @Qualifier("fireblocksHttpClient") OkHttpClient httpClient,
      FireblocksConfig fireblocksConfig) {
    return new FireblocksClient(httpClient, fireblocksConfig);
  }

  @Bean
  PaymentService paymentService(
      FireblocksClient fireblocksClient, FireblocksConfig fireblocksConfig) {
    return new FireblocksPaymentService(fireblocksClient, fireblocksConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "custody.fireblocks")
  FireblocksConfig fireblocksConfig(SecretConfig secretConfig) {
    return new FireblocksConfig(secretConfig);
  }
}
