package org.stellar.anchor.config;

import java.util.List;

@SuppressWarnings("unused")
public interface AppConfig {
  String getStellarNetworkPassPhrase();

  String getHostUrl();

  String getHorizonURI();

  String getJwtSecretKey();

  String getAssets();

  List<String> getLanguages();
}
