package org.stellar.anchor.platform.config;

import java.util.List;
import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.platform.service.SpringResourceReader;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.ResourceReader;
import org.stellar.anchor.util.UrlConnectionStatus;
import org.stellar.anchor.util.UrlValidationUtil;

@Data
public class PropertyAppConfig implements AppConfig, Validator {
  private String stellarNetworkPassphrase = "Test SDF Network ; September 2015";
  private String hostUrl = "http://localhost:9800";
  private String horizonUrl = "https://horizon-testnet.stellar.org";
  private String jwtSecretKey;
  private String assets = "assets-test.json";
  private List<String> languages;

  @Override
  public boolean supports(Class<?> clazz) {
    return AppConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    AppConfig config = (AppConfig) target;

    ValidationUtils.rejectIfEmpty(
        errors, "stellarNetworkPassphrase", "empty-stellarNetworkPassphrase");
    ValidationUtils.rejectIfEmpty(errors, "hostUrl", "empty-hostUrl");
    ValidationUtils.rejectIfEmpty(errors, "horizonUrl", "empty-horizonUrl");
    ValidationUtils.rejectIfEmpty(errors, "jwtSecretKey", "empty-jwtSecretKey");

    ResourceReader reader = new SpringResourceReader();
    if (!reader.checkResourceExists(config.getAssets())) {
      errors.rejectValue(
          "assets", "doesNotExist-assets", "assets resource file could not be found");
    }

    UrlConnectionStatus hostUrlStatus = UrlValidationUtil.validateUrl(config.getHostUrl());
    if (hostUrlStatus == UrlConnectionStatus.MALFORMED) {
      errors.rejectValue("hostUrl", "invalidUrl-hostUrl", "host url is not in valid format");
    } else if (hostUrlStatus == UrlConnectionStatus.UNREACHABLE) {
      Log.error("hostUrl field invalid: cannot connect to host url");
    }
    UrlConnectionStatus horizonUrlStatus = UrlValidationUtil.validateUrl(config.getHorizonUrl());
    if (horizonUrlStatus == UrlConnectionStatus.MALFORMED) {
      errors.rejectValue("horizonUrl", "invalidUrl-horizonUrl", "horizon url is not in valid format");
    } else if (horizonUrlStatus == UrlConnectionStatus.UNREACHABLE) {
      Log.error("horizonUrl field invalid: cannot connect to horizon url");
    }
  }
}
