package org.stellar.anchor.asset;

import static org.stellar.anchor.api.asset.AssetInfo.Schema.*;
import static org.stellar.anchor.util.AssetHelper.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.stellar.anchor.api.asset.AssetInfo;
import org.stellar.anchor.api.asset.FiatAssetInfo;
import org.stellar.anchor.api.asset.StellarAssetInfo;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.config.AssetsConfig;
import org.stellar.anchor.util.*;
import org.yaml.snakeyaml.Yaml;

@NoArgsConstructor
public class DefaultAssetService implements AssetService {
  static Gson gson = GsonUtils.getInstance();
  static StellarAssetInfo stellarAssetPrototype;
  static FiatAssetInfo fiatAssetPrototype;
  List<StellarAssetInfo> stellarAssets = new ArrayList<>();
  List<FiatAssetInfo> fiatAssets = new ArrayList<>();

  static {
    loadAssetPrototypes();
  }

  @SneakyThrows
  static void loadAssetPrototypes() {
    Gson protoGson = new Gson();
    DefaultAssetService prototypeDAS =
        fromYamlResource("config/anchor-asset-default-values.yaml", false);
    stellarAssetPrototype = (StellarAssetInfo) prototypeDAS.getAssetById("stellar:");
    fiatAssetPrototype = (FiatAssetInfo) prototypeDAS.getAssetById("iso4217:");
    GsonBuilder gsonBuilder =
        GsonUtils.builder()
            .registerTypeAdapter(
                StellarAssetInfo.class,
                (InstanceCreator<StellarAssetInfo>)
                    type -> protoGson.fromJson(protoGson.toJson(stellarAssetPrototype), type))
            .registerTypeAdapter(
                FiatAssetInfo.class,
                (InstanceCreator<FiatAssetInfo>)
                    type -> protoGson.fromJson(protoGson.toJson(fiatAssetPrototype), type));

    gson = gsonBuilder.create();
  }

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
    return createDASFromMap(map, true);
  }

  public static DefaultAssetService fromYamlContent(String assetsYaml)
      throws InvalidConfigException {
    return fromYamlContent(assetsYaml, true);
  }

  public static DefaultAssetService fromYamlContent(String assetsYaml, boolean validate)
      throws InvalidConfigException {
    Map<String, List<Object>> map = new Yaml().load(assetsYaml);
    return createDASFromMap(map, validate);
  }

  private static final String ASSETS_ROOT = "items";

  private static DefaultAssetService createDASFromMap(
      Map<String, List<Object>> map, boolean validate) throws InvalidConfigException {
    DefaultAssetService das = new DefaultAssetService();
    if (!map.containsKey(ASSETS_ROOT)) {
      throw new InvalidConfigException(String.format("Missing `%s` key in config", ASSETS_ROOT));
    }
    map.get(ASSETS_ROOT).removeIf(Objects::isNull);
    List<JsonObject> assetList =
        gson.fromJson(
            gson.toJson(map.get(ASSETS_ROOT)), new TypeToken<List<JsonObject>>() {}.getType());
    for (JsonObject asset : assetList) {
      String id = asset.get("id").getAsString();
      String schema = getAssetSchema(id);
      if (schema.equals(STELLAR.toString())) {
        StellarAssetInfo stellarAssetInfo =
            gson.fromJson(gson.toJson(asset), StellarAssetInfo.class);
        das.stellarAssets.add(stellarAssetInfo);
      } else if (schema.equals(ISO_4217.toString())) {
        FiatAssetInfo fiatAssetInfo = gson.fromJson(asset, FiatAssetInfo.class);
        das.fiatAssets.add(fiatAssetInfo);
      } else {
        throw new InvalidConfigException(String.format("Invalid asset: " + id));
      }
    }
    if (validate) {
      AssetValidator.validate(das);
    }
    return das;
  }

  public static DefaultAssetService fromJsonResource(String resourcePath)
      throws IOException, SepNotFoundException, InvalidConfigException {
    return fromJsonContent(FileUtil.getResourceFileAsString(resourcePath));
  }

  public static DefaultAssetService fromYamlResource(String resourcePath)
      throws IOException, InvalidConfigException, SepNotFoundException {
    return fromYamlResource(resourcePath, true);
  }

  public static DefaultAssetService fromYamlResource(String resourcePath, boolean validate)
      throws IOException, SepNotFoundException, InvalidConfigException {
    return fromYamlContent(FileUtil.getResourceFileAsString(resourcePath), validate);
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
  public List<AssetInfo> getAssets() {
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
