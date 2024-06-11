package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.Log.error;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.client.DefaultClientService;
import org.stellar.anchor.config.ClientsConfig;
import org.stellar.anchor.util.StringHelper;

@Data
public class PropertyClientsConfig implements ClientsConfig, Validator {
  ClientsConfigType type;
  String value;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return ClientsConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    // TODO(jiahui): ANCHOR-622 Support more types of clients config
    if (this.getType() == null) {
      errors.reject("invalid-no-type-defined", "clients.type is empty. Please define.");
    }
    if (StringHelper.isEmpty(this.getValue())) {
      errors.reject("invalid-no-value-defined", "clients.value is empty. Please define.");
    } else {
      switch (this.getType()) {
        case FILE:
          try {
            DefaultClientService.fromClientsConfig(this);
          } catch (Exception ex) {
            error("Error loading clients file", ex);
            errors.reject(
                "clients-file-not-valid", "Cannot read from clients file: " + this.getValue());
          }
          break;
        case YAML:
          try {
            DefaultClientService.fromYamlResourceFile(this.getValue());
          } catch (Exception ex) {
            error("Error loading clients YAML", ex);
            errors.reject(
                "invalid-clients-yaml-format",
                "clients.value does not contain a valid YAML string for clients");
          }
          break;
        default:
          errors.reject(
              "invalid-type-defined",
              String.format("clients.type:%s is not supported.", this.getType()));
          break;
      }
    }
  }
}
