package org.stellar.anchor.config;

import org.springframework.validation.Errors;

public interface CircleConfig {
  String getCircleUrl();

  String getApiKey();

  Errors validate();
}
