package org.stellar.anchor.platform.config;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.config.Sep10Config;

@Data
public class PropertySep10Config implements Sep10Config {
  private String homeDomain;
  private boolean clientAttributionRequired = false;
  private Boolean enabled = true;
  private String signingSeed;
  private Integer authTimeout = 900;
  private Integer jwtTimeout = 86400;
  private List<String> clientAttributionDenyList;
  private List<String> clientAttributionAllowList;
  private List<String> omnibusAccountList;
  private boolean requireKnownOmnibusAccount;
}
