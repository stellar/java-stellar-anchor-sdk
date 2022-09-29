package org.stellar.anchor.platform.configurator;

import static org.stellar.anchor.util.Log.info;
import static org.stellar.anchor.util.StringHelper.isNotEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

public class SecretManager
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  List<String> secretVars =
      Arrays.asList(
          "secret.sep10.jwt_secret",
          "secret.sep10.signing_seed",
          "secret.callback_api.auth_secret",
          "secret.platform_api.auth_secret",
          "secret.circle.api_key");

  static SecretManager secretManager = new SecretManager();

  private SecretManager() {}

  public static SecretManager getInstance() {
    return secretManager;
  }

  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    info("Secret manager started.");
    Properties props = new Properties();
    secretVars.forEach(
        var -> {
          String secret = ConfigEnvironment.getenv(var);
          if (isNotEmpty(secret)) {
            props.put(var, secret);
          }
        });
    // Set Platform configurations
    PropertiesPropertySource pps = new PropertiesPropertySource("secret", props);
    applicationContext.getEnvironment().getPropertySources().addFirst(pps);
  }
}
