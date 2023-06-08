package org.stellar.anchor.platform.config;

import java.io.File;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.Sep1Config;
import org.stellar.anchor.util.NetUtil;
import org.stellar.anchor.util.Sep1Helper;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertySep1Config implements Sep1Config, Validator {
  boolean enabled;
  TomlConfig toml;

  @Override
  public TomlType getType() {
    return toml.getType();
  }

  @Override
  public String getValue() {
    return toml.getValue();
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class TomlConfig {
    TomlType type;
    String value;
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return Sep1Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    PropertySep1Config config = (PropertySep1Config) target;

    if (config.isEnabled()) {
      validateConfig(config, errors);
      if (!errors.hasErrors()) validateTomlTypeAndValue(config, errors);
    }
  }

  void validateTomlTypeAndValue(PropertySep1Config config, Errors errors) {
    if (config.getToml().getType() == null) {
      errors.rejectValue("type", "sep1-toml-type-empty", "sep1.toml.type must not be empty");
    } else {
      switch (config.getToml().getType()) {
        case STRING:
          try {
            Sep1Helper.parse(config.getToml().getValue());
          } catch (IllegalStateException isex) {
            errors.rejectValue(
                "value",
                "sep1-toml-value-string-invalid",
                String.format(
                    "sep1.toml.value does not contain a valid TOML. %s", isex.getMessage()));
          }
          break;
        case URL:
          if (!NetUtil.isUrlValid(config.getToml().getValue())) {
            errors.rejectValue(
                "value",
                "sep1-toml-value-url-invalid",
                String.format(
                    "sep1.toml.value=%s is not a valid URL", config.getToml().getValue()));
          }
          break;
        case FILE:
          File file = new File(config.getToml().getValue());
          if (!file.exists()) {
            errors.rejectValue(
                "value",
                "sep1-toml-value-file-does-not-exist",
                String.format(
                    "sep1.toml.value=%s specifies a file that does not exist",
                    config.getToml().getValue()));
          }
          break;
        default:
          errors.rejectValue(
              "type",
              "sep1-toml-type-invalid",
              String.format(
                  "'%s' is not a valid sep1.toml.type. Only 'string', 'url' and 'file' are supported",
                  config.getToml().getValue()));
          break;
      }
    }
  }

  void validateConfig(Sep1Config ignoredConfig, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors, "type", "sep1-toml-type-empty", "sep1.toml.type is must be specified");
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors, "value", "sep1-toml-value-empty", "sep1.toml.value is must be specified");
  }
}
