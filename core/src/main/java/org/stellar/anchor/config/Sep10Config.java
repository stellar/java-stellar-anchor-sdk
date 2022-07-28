package org.stellar.anchor.config;

import java.util.List;

public interface Sep10Config {
  String getHomeDomain();

  boolean isClientAttributionRequired();

  Boolean getEnabled();

  @Secret
  String getSigningSeed();

  Integer getAuthTimeout();

  Integer getJwtTimeout();

  List<String> getClientAttributionDenyList();

  List<String> getClientAttributionAllowList();

  List<String> getOmnibusAccountList();

  boolean isRequireKnownOmnibusAccount();
}
