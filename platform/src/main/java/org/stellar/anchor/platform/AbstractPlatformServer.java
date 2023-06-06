package org.stellar.anchor.platform;

import static org.stellar.anchor.util.Log.info;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.format.FormatterRegistry;
import org.stellar.anchor.platform.configurator.ConfigEnvironment;
import org.stellar.anchor.platform.utils.StringEnumConverter;

abstract class AbstractPlatformServer {
  ConfigurableApplicationContext ctx;

  void buildEnvironment(Map<String, String> envMap) {
    info("Building Anchor Platform environment...");
    ConfigEnvironment.rebuild(envMap);
  }

  public void stop() {
    if (ctx != null) {
      SpringApplication.exit(ctx);
      ctx = null;
    }
  }

  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new StringEnumConverter.TransactionsOrderByConverter());
    registry.addConverter(new StringEnumConverter.TransactionsSepsConverter());
    registry.addConverter(new StringEnumConverter.SepTransactionStatusConverter());
    registry.addConverter(new StringEnumConverter.DirectionConverter());
  }
}
