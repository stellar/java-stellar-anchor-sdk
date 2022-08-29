package org.stellar.anchor.platform;

import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.util.GsonUtils;

@Configuration
public class HelperConfig {
  @Bean
  public Gson gson() {
    return GsonUtils.builder().create();
  }
}
