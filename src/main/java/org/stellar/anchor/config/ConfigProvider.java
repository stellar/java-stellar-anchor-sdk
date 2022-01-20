package org.stellar.anchor.config;

public interface ConfigProvider {
    AppConfig appConfig();
    Sep1Config sep1Config();
    Sep10Config sep10Config();
    Sep24Config sep24Config();
}
