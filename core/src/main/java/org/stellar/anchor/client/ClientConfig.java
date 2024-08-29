package org.stellar.anchor.client;

import lombok.Builder;
import lombok.Data;

public interface ClientConfig {
  String getName();

  String getCallbackUrl();

  CallbackUrls getCallbackUrls();

  @Data
  @Builder
  class CallbackUrls {
    private String sep6;
    private String sep24;
    private String sep31;
    private String sep12;
  }
}
