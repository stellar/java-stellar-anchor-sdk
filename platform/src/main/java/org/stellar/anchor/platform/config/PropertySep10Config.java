package org.stellar.anchor.platform.config;

import java.util.List;
import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.Sep10Config;

@Data
public class PropertySep10Config implements Sep10Config, Validator {
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

  @Override
  public boolean supports(Class<?> clazz) {
    return Sep10Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    Sep10Config config = (Sep10Config) target;

    if (config.getEnabled()) {
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "homeDomain", "empty-homeDomain");
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "signingSeed", "empty-signingSeed");
    }
  }
}
