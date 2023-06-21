package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.Log.error;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.api.sep.AssetInfo;
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
    if (this.getType() == null) {
      errors.reject("invalid-no-type-defined", "assets.type is empty. Please define.");
    }

    if (StringHelper.isEmpty(this.getValue())) {
      errors.reject("invalid-no-value-defined", "assets.value is empty. Please define.");
    } else {
      DefaultAssetService assetService = null;
      switch (this.getType()) {
        case JSON:
          try {
            assetService = DefaultAssetService.fromJson(this.getValue());
          } catch (Exception ex) {
            error("Error loading asset JSON", ex);
            errors.reject(
                "invalid-asset-json-format",
                "assets.value does not contain a valid JSON string for assets");
          }
          break;
        case YAML:
          try {
            assetService = DefaultAssetService.fromYaml(this.getValue());
          } catch (Exception ex) {
            error("Error loading asset YAML", ex);
            errors.reject(
                "invalid-asset-yaml-format",
                "assets.value does not contain a valid YAML string for assets");
          }
          break;
        case FILE:
          try {
            assetService = DefaultAssetService.fromAssetConfig(this);
          } catch (Exception ex) {
            error("Error loading asset file", ex);
            errors.reject(
                "assets-file-not-valid", "Cannot read from asset file: " + this.getValue());
          }
          break;
        case URL:
        default:
          errors.reject(
              "invalid-type-defined",
              String.format("assets.type:%s is not supported.", this.getType()));
          break;
      }

      assert assetService != null;
      List<AssetInfo> assets = assetService.listAllAssets();
      Set<String> existingAssetNames = new HashSet<>();
      for (AssetInfo asset : assets) {
        if (!existingAssetNames.add(asset.getAssetName())) {
          error("Error asset already exist", asset.getAssetName());
          errors.reject(
              "invalid-asset-duplicate-exists",
              String.format("assets %s already exists.", asset.getAssetName()));
        }
      }
    }
  }
}
