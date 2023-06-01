package org.stellar.anchor.config;

import java.util.List;

public interface Sep10Config {
  String getWebAuthDomain();

  String getHomeDomain();

  boolean isClientAttributionRequired();

  Boolean getEnabled();

  Integer getAuthTimeout();

  Integer getJwtTimeout();

  List<String> getClientAttributionDenyList();

  List<String> getClientAttributionAllowList();

  List<String> getOmnibusAccountList();

  boolean isRequireKnownOmnibusAccount();
}
