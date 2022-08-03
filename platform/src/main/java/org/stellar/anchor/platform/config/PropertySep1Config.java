package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.Sep1Config;
import org.stellar.anchor.platform.service.SpringResourceReader;
import org.stellar.anchor.util.ResourceReader;

@Data
public class PropertySep1Config implements Sep1Config, Validator {
  String stellarFile;
  boolean enabled = false;

  @Override
  public boolean supports(Class<?> clazz) {
    return Sep1Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    Sep1Config config = (Sep1Config) target;

    if (config.isEnabled()) {
      ResourceReader reader = new SpringResourceReader();
      if (!reader.checkResourceExists(config.getStellarFile())) {
        errors.rejectValue(
            "stellarFile", "doesNotExist-stellarFile", "stellarFile resource does not resolve");
      }
    }
  }
}
