package org.stellar.anchor.server.config;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.config.AppConfig;

@Data
public class PropertyAppConfig implements AppConfig {
  private String stellarNetworkPassphrase = "Test SDF Network ; September 2015";
  private String hostUrl = "http://localhost:9800";
  private String horizonUrl = "https://horizon-testnet.stellar.org";

  private String jwtSecretKey;
  private String assets = "assets-test.json";

  private List<String> languages;
}
