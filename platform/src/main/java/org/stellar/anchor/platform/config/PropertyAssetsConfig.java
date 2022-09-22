package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.AssetsConfig;

@Data
public class PropertyAssetsConfig implements AssetsConfig, Validator {
  AssetConfigType type;
  String value;

  @Override
  public boolean supports(Class<?> clazz) {
    return AssetsConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {}
}
