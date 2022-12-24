package org.stellar.anchor.platform.component.share;

import com.google.gson.Gson;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AssetsConfig;
import org.stellar.anchor.healthcheck.HealthCheckable;
import org.stellar.anchor.platform.data.PaymentObservingAccountRepo;
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountStore;
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountsManager;
import org.stellar.anchor.platform.service.HealthCheckService;
import org.stellar.anchor.platform.service.PropertyAssetsService;
import org.stellar.anchor.util.GsonUtils;

@Configuration
public class HelperBeans {
  @Bean
  public Gson gson() {
    return GsonUtils.builder().create();
  }

  @Bean
  @DependsOn("configManager")
  public HealthCheckService healthCheckService(List<HealthCheckable> checkables) {
    return new HealthCheckService(checkables);
  }

  @Bean
  public PaymentObservingAccountsManager observingAccounts(
      PaymentObservingAccountStore paymentObservingAccountStore) {
    return new PaymentObservingAccountsManager(paymentObservingAccountStore);
  }

  @Bean
  public PaymentObservingAccountStore observingAccountStore(PaymentObservingAccountRepo repo) {
    return new PaymentObservingAccountStore(repo);
  }

  @Bean
  AssetService assetService(AssetsConfig assetsConfig) throws InvalidConfigException {
    return new PropertyAssetsService(assetsConfig);
  }
}
