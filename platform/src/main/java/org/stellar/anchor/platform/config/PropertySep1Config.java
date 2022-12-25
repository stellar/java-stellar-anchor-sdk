package org.stellar.anchor.platform.config;

import java.io.File;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${sep1.toml.type:#{null}}")
  TomlType type;

  @Value("${sep1.toml.value:#{null}}")
  String value;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return Sep1Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    Sep1Config config = (Sep1Config) target;

    if (config.isEnabled()) {
      validateConfig(config, errors);
      if (!errors.hasErrors()) validateTomlTypeAndValue(config, errors);
    }
  }

  void validateTomlTypeAndValue(Sep1Config config, Errors errors) {
    if (config.getType() == null) {
      errors.rejectValue("type", "sep1-toml-type-empty", "sep1.toml.type must not be empty");
    } else {
      switch (config.getType()) {
        case STRING:
          try {
            Sep1Helper.parse(config.getValue());
          } catch (IllegalStateException isex) {
            errors.rejectValue(
                "value",
                "sep1-toml-value-string-invalid",
                String.format(
                    "sep1.toml.value does not contain a valid TOML. %s", isex.getMessage()));
          }
          break;
        case URL:
          if (!NetUtil.isUrlValid(config.getValue())) {
            errors.rejectValue(
                "value",
                "sep1-toml-value-url-invalid",
                String.format("sep1.toml.value=%s is not a valid URL", config.getValue()));
          }
          break;
        case FILE:
          File file = new File(config.getValue());
          if (!file.exists()) {
            errors.rejectValue(
                "value",
                "sep1-toml-value-file-does-not-exist",
                String.format(
                    "sep1.toml.value=%s specifies a file that does not exist", config.getValue()));
          }
          break;
        default:
          errors.rejectValue(
              "type",
              "sep1-toml-type-invalid",
              String.format(
                  "'%s' is not a valid sep1.toml.type. Only 'string', 'url' and 'file' are supported",
                  config.getValue()));
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
