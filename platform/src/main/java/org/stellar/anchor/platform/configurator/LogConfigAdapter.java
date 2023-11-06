package org.stellar.anchor.platform.configurator;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.slf4j.Log4jLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.anchor.api.exception.InvalidConfigException;

public class LogConfigAdapter extends SpringConfigAdapter {

  @Override
  void updateSpringEnv(ConfigMap config) throws InvalidConfigException {
    copy(config, "app_logging.level", "logging.level.root");
    copy(config, "app_logging.stellar_level", "logging.level.org.stellar");

    // Check the logger type
    Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    if (logger instanceof Log4jLogger) {
      // we are using log4j
      if (config.get("app_logging.level") != null) {
        Level rootLevel = getLog4j2Level(config.getString("app_logging.level", "INFO"));
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), rootLevel);
      }

      if (config.get("app_logging.stellar_level") != null) {
        Level stellarLevel = getLog4j2Level(config.getString("app_logging.stellar_level"));
        Configurator.setAllLevels("org.stellar", stellarLevel);
      }
    }
  }

  @Override
  void validate(ConfigMap config) throws InvalidConfigException {}

  private Level getLog4j2Level(String level) throws InvalidConfigException {
    switch (level.toUpperCase()) {
      case "TRACE":
      case "DEBUG":
      case "INFO":
      case "WARN":
      case "ERROR":
      case "FATAL":
        return Level.getLevel(level.toUpperCase());
      default:
        throw new InvalidConfigException("Invalid config[logger.level]=" + level);
    }
  }
}
