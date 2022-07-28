package org.stellar.anchor.reference.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "anchor.settings")
public class AppSettings {
  String version;
  String platformApiEndpoint;
  String hostUrl;
  String distributionWallet;
  String distributionWalletMemo;
  String distributionWalletMemoType;
}
