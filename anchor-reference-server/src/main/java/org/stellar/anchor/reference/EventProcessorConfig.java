package org.stellar.anchor.reference;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.reference.event.*;
import org.stellar.anchor.reference.event.processor.MemoryTransactionStore;
import org.stellar.anchor.reference.event.processor.NoopEventProcessor;
import org.stellar.anchor.reference.event.processor.Sep24EventProcessor;
import org.stellar.anchor.reference.event.processor.Sep6EventProcessor;
import org.stellar.anchor.reference.service.CustomerService;
import org.stellar.sdk.Server;

@Configuration
public class EventProcessorConfig {
  @Bean
  public PlatformApiClient platformApiClient(AppSettings appSettings, AuthHelper authHelper) {
    return new PlatformApiClient(authHelper, appSettings.getPlatformApiEndpoint());
  }

  @Bean
  public Server server() {
    return new Server("https://horizon-testnet.stellar.org");
  }

  @Bean
  public ActiveTransactionStore activeTransactionStore() {
    return new MemoryTransactionStore();
  }

  @Bean
  public Sep6EventProcessor sep6EventProcessor(
      AppSettings appSettings,
      PlatformApiClient platformApiClient,
      Server server,
      CustomerService customerService,
      ActiveTransactionStore activeTransactionStore) {
    return new Sep6EventProcessor(
        appSettings, platformApiClient, server, customerService, activeTransactionStore);
  }

  @Bean
  public Sep24EventProcessor sep24EventProcessor() {
    return new Sep24EventProcessor();
  }

  @Bean
  public NoopEventProcessor noopEventProcessor() {
    return new NoopEventProcessor();
  }
}
