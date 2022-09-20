package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.StringHelper.isNotEmpty;

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
  String type;

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
      ValidationUtils.rejectIfEmptyOrWhitespace(
          errors,
          "type",
          "empty-sep1Type",
          "The value of config[sep1.toml.type] is must be specified");
      ValidationUtils.rejectIfEmptyOrWhitespace(
          errors,
          "value",
          "empty-sep1Value",
          "The value of config[sep1.toml.value] is must be specified");

      if (isNotEmpty(config.getValue()) && isNotEmpty(config.getType())) {
        switch (config.getType().toLowerCase()) {
          case "string":
            try {
              Sep1Helper.parse(config.getValue());
            } catch (IllegalStateException isex) {
              errors.rejectValue(
                  "value",
                  "invalid-sep1Value",
                  String.format(
                      "config[sep1.toml.value] does not contain a valid TOML. %s",
                      isex.getMessage()));
            }
            break;
          case "url":
            if (!NetUtil.isUrlValid(config.getValue())) {
              errors.rejectValue(
                  "value",
                  "invalid-sep1Value",
                  String.format(
                      "config[sep1.toml.value]=%s is not a valid URL", config.getValue()));
            }
            break;
          case "file":
            File file = new File(config.getValue());
            if (!file.exists()) {
              errors.rejectValue(
                  "value",
                  "doesNotExist-sep1Value",
                  String.format(
                      "config[sep1.toml.value]=%s specifies a file that does not exist",
                      config.getValue()));
            }
            break;
          default:
            errors.rejectValue(
                "type",
                "invalid-sep1Type",
                String.format(
                    "'%s' is not a valid config[sep1.toml.type]. Only 'string', 'url' and 'file' are supported",
                    config.getValue()));
            break;
        }
      } else {
        ValidationUtils.rejectIfEmptyOrWhitespace(
                errors,
                "value",
                "empty-sep1TypeOrValue",
                "Both the [sep1.toml.type] and [sep1.toml.value] must be specified if enabled");
      }
    }
  }
}
