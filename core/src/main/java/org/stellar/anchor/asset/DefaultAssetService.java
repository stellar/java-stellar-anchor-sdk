package org.stellar.anchor.asset;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.stellar.anchor.api.asset.AssetInfo;
import org.stellar.anchor.api.asset.FiatAssetInfo;
import org.stellar.anchor.api.asset.StellarAssetInfo;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.config.AssetsConfig;
import org.stellar.anchor.util.AssetHelper;
import org.stellar.anchor.util.FileUtil;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;
import org.yaml.snakeyaml.Yaml;

@NoArgsConstructor
public class DefaultAssetService implements AssetService {
  static final Gson gson = GsonUtils.getInstance();
  List<StellarAssetInfo> stellarAssets = new ArrayList<>();
  List<FiatAssetInfo> fiatAssets = new ArrayList<>();

  public static DefaultAssetService fromAssetConfig(AssetsConfig assetsConfig)
      throws InvalidConfigException {
    switch (assetsConfig.getType()) {
      case JSON:
        return fromJsonContent(assetsConfig.getValue());
      case YAML:
        return fromYamlContent(assetsConfig.getValue());
      case FILE:
        String filename = assetsConfig.getValue();
        try {
          String content = FileUtil.read(Path.of(filename));
          switch (FilenameUtils.getExtension(filename).toLowerCase()) {
            case "json":
              return fromJsonContent(content);
            case "yaml":
            case "yml":
              return fromYamlContent(content);
            default:
              throw new InvalidConfigException(
                  String.format("%s is not a supported file format", filename));
          }
        } catch (Exception ex) {
          throw new InvalidConfigException(
              List.of(String.format("Cannot read from asset file: %s", filename)), ex);
        }
      default:
        Log.infoF("assets type {} is not supported", assetsConfig.getType());
        throw new InvalidConfigException(
            String.format("assets type %s is not supported", assetsConfig.getType()));
    }
  }

  public static DefaultAssetService fromJsonContent(String assetsJson)
      throws InvalidConfigException {
    Map<String, List<Object>> map =
        gson.fromJson(assetsJson, new TypeToken<Map<String, List<Object>>>() {}.getType());
    return createDASFromMap(map);
  }

  public static DefaultAssetService fromYamlContent(String assetsYaml)
      throws InvalidConfigException {
    Map<String, List<Object>> map = new Yaml().load(assetsYaml);
    return createDASFromMap(map);
  }

  private static DefaultAssetService createDASFromMap(Map<String, List<Object>> map)
      throws InvalidConfigException {
    DefaultAssetService das = new DefaultAssetService();
    map.get("assets").removeIf(Objects::isNull);
    List<JsonObject> assetList =
        gson.fromJson(
            gson.toJson(map.get("assets")), new TypeToken<List<JsonObject>>() {}.getType());
    for (JsonObject asset : assetList) {
      String id = asset.get("id").getAsString();
      String schema = AssetHelper.getAssetSchema(id);
      if (schema.equals(AssetInfo.Schema.STELLAR.toString())) {
        StellarAssetInfo stellarAssetInfo =
            gson.fromJson(gson.toJson(asset), StellarAssetInfo.class);
        das.stellarAssets.add(stellarAssetInfo);
      } else if (schema.equals(AssetInfo.Schema.ISO_4217.toString())) {
        FiatAssetInfo fiatAssetInfo = gson.fromJson(asset, FiatAssetInfo.class);
        das.fiatAssets.add(fiatAssetInfo);
      } else {
        throw new InvalidConfigException(String.format("Invalid asset: " + id));
      }
    }
    AssetServiceValidator.validate(das);
    return das;
  }

  public static DefaultAssetService fromJsonResource(String resourcePath)
      throws IOException, SepNotFoundException, InvalidConfigException {
    return fromJsonContent(FileUtil.getResourceFileAsString(resourcePath));
  }

  public static DefaultAssetService fromYamlResource(String resourcePath)
      throws IOException, SepNotFoundException, InvalidConfigException {
    return fromYamlContent(FileUtil.getResourceFileAsString(resourcePath));
  }

  @Override
  public List<StellarAssetInfo> getStellarAssets() {
    return stellarAssets;
  }

  @Override
  public List<FiatAssetInfo> getFiatAssets() {
    return fiatAssets;
  }

  @Override
  public List<AssetInfo> getAllAssets() {
    List<AssetInfo> allAssets = new ArrayList<>();
    allAssets.addAll(stellarAssets);
    allAssets.addAll(fiatAssets);
    return allAssets;
  }

  @Override
  public AssetInfo getAssetById(String id) {
    for (AssetInfo asset : stellarAssets) {
      if (asset.getId().equals(id)) {
        return asset;
      }
    }
    for (AssetInfo asset : fiatAssets) {
      if (asset.getId().equals(id)) {
        return asset;
      }
    }
    return null;
  }

  @Override
  public AssetInfo getAsset(String code) {
    for (AssetInfo asset : stellarAssets) {
      if (asset.getCode().equals(code)) {
        return asset;
      }
    }
    for (AssetInfo asset : fiatAssets) {
      if (asset.getCode().equals(code)) {
        return asset;
      }
    }
    return null;
  }

  @Override
  public AssetInfo getAsset(String code, String issuer) {
    if (issuer == null) return getAsset(code);
    for (AssetInfo asset : stellarAssets) {
      if (asset.getCode().equals(code) && asset.getIssuer().equals(issuer)) {
        return asset;
      }
    }
    for (AssetInfo asset : fiatAssets) {
      if (asset.getCode().equals(code) && asset.getIssuer().equals(issuer)) {
        return asset;
      }
    }
    return null;
  }
}
