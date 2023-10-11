package org.stellar.anchor.platform.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.Errors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HttpClientConfig {

  private int connectTimeout;
  private int readTimeout;
  private int writeTimeout;
  private int callTimeout;

  public void validate(String configPrefix, Errors errors) {
    if (connectTimeout < 0) {
      errors.reject(
          configPrefix + "-http-client-connect-timeout-invalid",
          configPrefix + "-http-client-connect-timeout must be greater than or equal to 0");
    }
    if (readTimeout < 0) {
      errors.reject(
          configPrefix + "-http-client-read-timeout-invalid",
          configPrefix + "-http-client-read-timeout must be greater than or equal to 0");
    }
    if (writeTimeout < 0) {
      errors.reject(
          configPrefix + "-http-client-write-timeout-invalid",
          configPrefix + "-http-client-write-timeout must be greater than or equal to 0");
    }
    if (callTimeout < 0) {
      errors.reject(
          configPrefix + "-http-client-call-timeout-invalid",
          configPrefix + "-http-client-call-timeout must be greater than or equal to 0");
    }
  }
}
