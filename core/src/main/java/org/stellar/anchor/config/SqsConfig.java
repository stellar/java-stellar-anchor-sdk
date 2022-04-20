package org.stellar.anchor.config;

import java.util.Map;

public interface SqsConfig {
    String getRegion();
    Map<String, String> getEventTypeToQueue();
    String getAccessKey();
    String getSecretKey();
}
core/src/main/java/org/stellar/anchor/config/SqsConfig.java