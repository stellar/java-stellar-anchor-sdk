package org.stellar.anchor.platform.configurator;

import static org.stellar.anchor.platform.config.PropertyCustodySecretConfig.SECRET_CUSTODY_SERVER_AUTH_SECRET;
import static org.stellar.anchor.platform.config.PropertyCustodySecretConfig.SECRET_FIREBLOCKS_API_KEY;
import static org.stellar.anchor.platform.config.PropertyCustodySecretConfig.SECRET_FIREBLOCKS_SECRET_KEY;
import static org.stellar.anchor.platform.config.PropertySecretConfig.*;
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
  final List<String> secretVars =
      Arrays.asList(
          SECRET_SEP_10_JWT_SECRET,
          SECRET_SEP_10_SIGNING_SEED,
          SECRET_SEP_24_INTERACTIVE_URL_JWT_SECRET,
          SECRET_SEP_24_MORE_INFO_URL_JWT_SECRET,
          SECRET_CALLBACK_API_AUTH_SECRET,
          SECRET_PLATFORM_API_AUTH_SECRET,
          SECRET_CUSTODY_SERVER_AUTH_SECRET,
          SECRET_DATA_USERNAME,
          SECRET_DATA_PASSWORD,
          SECRET_FIREBLOCKS_SECRET_KEY,
          SECRET_FIREBLOCKS_API_KEY);

  final Properties props = new Properties();

  static final SecretManager secretManager = new SecretManager();

  private SecretManager() {}

  public static SecretManager getInstance() {
    return secretManager;
  }

  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    info("Secret manager started.");
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

  public String get(String key) {
    return props.getProperty(key);
  }
}
