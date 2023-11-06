package org.stellar.anchor.platform.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Data
@Configuration
@ConfigurationProperties(prefix = "queue.kafka")
public class EventProcessorConfig implements Validator {

  ClientStatusCallbackConfig clientStatusCallback;
  CallbackApiRequestConfig callbackApiRequest;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return EventProcessorConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {}

  @Data
  public static class ClientStatusCallbackConfig {
    boolean enabled;
  }

  @Data
  public static class CallbackApiRequestConfig {
    boolean enabled;
  }
}
