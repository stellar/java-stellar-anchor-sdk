package org.stellar.anchor.client.component.share;

import java.io.IOException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.asset.DefaultAssetService;
import org.stellar.anchor.client.config.PropertyAssetsConfig;
import org.stellar.anchor.config.AssetsConfig;

@Configuration
public class AssetBeans {
  @Bean
  @ConfigurationProperties(prefix = "assets")
  AssetsConfig assetsConfig() {
    return new PropertyAssetsConfig();
  }

  @Bean
  AssetService assetService(AssetsConfig assetsConfig) throws InvalidConfigException, IOException {
    return DefaultAssetService.fromAssetConfig(assetsConfig);
  }
}
