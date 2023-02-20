package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.Log.error;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.asset.DefaultAssetService;
import org.stellar.anchor.config.AssetsConfig;
import org.stellar.anchor.util.StringHelper;

@Data
public class PropertyAssetsConfig implements AssetsConfig, Validator {
  AssetConfigType type;
  String value;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return AssetsConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    PropertyAssetsConfig config = (PropertyAssetsConfig) target;
    checkType(config, errors);
    checkValue(config, errors);
  }

  void checkValue(PropertyAssetsConfig config, Errors errors) {
    if (StringHelper.isEmpty(config.getValue())) {
      errors.reject("invalid-no-value-defined", "assets.value is empty. Please define.");
    } else {
      switch (config.getType()) {
        case JSON:
          try {
            DefaultAssetService.fromJson(config.getValue());
          } catch (Exception jsex) {
            error("Error loading asset JSON", jsex);
            errors.reject(
                "invalid-asset-json-format",
                "assets.value does not contain a valid JSON string for assets");
          }
          break;
        case YAML:
          try {
            DefaultAssetService.fromYaml(config.getValue());
          } catch (Exception jsex) {
            error("Error loading asset YAML", jsex);
            errors.reject(
                "invalid-asset-yaml-format",
                "assets.value does not contain a valid YAML string for assets");
          }
          break;
        case FILE:
          try {
            DefaultAssetService.fromAssetConfig(config);
          } catch (Exception ex) {
            errors.reject(
                "assets-value-file-not-valid", "Cannot read from asset file: " + config.getValue());
          }
        case URL:
        default:
          break;
      }
    }
  }

  void checkType(PropertyAssetsConfig config, Errors errors) {
    if (config.getType() == null) {
      errors.reject("invalid-no-type-defined", "assets.type is empty. Please define.");
    }
    switch (config.getType()) {
      case JSON:
      case YAML:
      case FILE:
        break;
      case URL:
      default:
        errors.reject(
            "invalid-type-defined",
            String.format("assets.type:%s is not supported.", config.getType()));
    }
  }
}
