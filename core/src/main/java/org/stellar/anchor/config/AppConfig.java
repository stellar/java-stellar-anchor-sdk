package org.stellar.anchor.config;

import java.util.List;

public interface AppConfig {
  String getStellarNetworkPassphrase();

  String getHostUrl();

  String getHorizonUrl();

  List<String> getLanguages();
}
