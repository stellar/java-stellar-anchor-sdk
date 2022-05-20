package org.stellar.anchor.config;

import java.util.List;

public interface AppConfig {
  String getStellarNetworkPassphrase();

  String getHostUrl();

  String getHorizonUrl();

  @Secret
  String getJwtSecretKey();

  String getAssets();

  List<String> getLanguages();
}
