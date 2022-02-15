package org.stellar.anchor.config;

import java.util.List;

@SuppressWarnings("unused")
public interface AppConfig {
  String getStellarNetworkPassphrase();

  String getHostUrl();

  String getHorizonUrl();

  String getJwtSecretKey();

  String getAssets();

  List<String> getLanguages();
}
