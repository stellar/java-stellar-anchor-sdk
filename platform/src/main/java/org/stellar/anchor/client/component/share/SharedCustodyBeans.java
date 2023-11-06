package org.stellar.anchor.client.component.share;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.apiclient.CustodyApiClient;
import org.stellar.anchor.client.service.CustodyServiceImpl;
import org.stellar.anchor.custody.CustodyService;

@Configuration
public class SharedCustodyBeans {

  @Bean
  CustodyService custodyService(Optional<CustodyApiClient> custodyApiClient) {
    return new CustodyServiceImpl(custodyApiClient);
  }
}
