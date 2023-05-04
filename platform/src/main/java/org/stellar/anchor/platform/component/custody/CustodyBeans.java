package org.stellar.anchor.platform.component.custody;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.custody.CustodyTransactionService;
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo;

@Configuration
public class CustodyBeans {

  @Bean
  CustodyTransactionService getCustodyTransactionService(
      JdbcCustodyTransactionRepo custodyTransactionRepo) {
    return new CustodyTransactionService(custodyTransactionRepo);
  }
}
