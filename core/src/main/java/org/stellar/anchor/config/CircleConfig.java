package org.stellar.anchor.config;

public interface CircleConfig {
  String getCircleUrl();

  String getApiKey();

  boolean validate();
}
