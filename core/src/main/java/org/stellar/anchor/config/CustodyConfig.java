package org.stellar.anchor.config;

public interface CustodyConfig {

  String NONE_CUSTODY_TYPE = "none";

  default boolean isCustodyIntegrationEnabled() {
    return !NONE_CUSTODY_TYPE.equals(getType());
  }

  String getType();
}
