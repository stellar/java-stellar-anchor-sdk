package org.stellar.anchor.asset;

import static org.stellar.anchor.util.Log.error;
import static org.stellar.anchor.util.Log.infoF;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.config.AssetsConfig;
import org.stellar.anchor.util.FileUtil;
import org.stellar.anchor.util.GsonUtils;

@NoArgsConstructor
public class DefaultAssetService implements AssetService {
  static final Gson gson = GsonUtils.getInstance();
  Assets assets;

  public static DefaultAssetService fromAssetConfig(AssetsConfig assetsConfig)
      throws InvalidConfigException {
    switch (assetsConfig.getType()) {
      case JSON:
        return fromJson(assetsConfig.getValue());
      case YAML:
      default:
        infoF("assets type {} is not supported", assetsConfig.getType());
        throw new InvalidConfigException(
            String.format("assets type %s is not supported", assetsConfig.getType()));
    }
  }

  public static DefaultAssetService fromJson(String assetsJson) throws InvalidConfigException {
    DefaultAssetService assetService = new DefaultAssetService();
    assetService.assets = gson.fromJson(assetsJson, Assets.class);
    if (assetService.assets == null
        || assetService.assets.getAssets() == null
        || assetService.assets.getAssets().size() == 0) {
      error("Invalid asset defined. assets JSON=", assetsJson);
      throw new InvalidConfigException(
          "Invalid assets defined in configuration. Please check the logs for details.");
    }
    return assetService;
  }

  public static DefaultAssetService fromResource(String assetPath)
      throws IOException, SepNotFoundException, InvalidConfigException {
    return fromJson(FileUtil.getResourceFileAsString(assetPath));
  }

  public List<AssetInfo> listAllAssets() {
    return new ArrayList<>(assets.getAssets());
  }

  public AssetInfo getAsset(String code) {
    for (AssetInfo asset : assets.getAssets()) {
      if (asset.getCode().equals(code)) {
        return asset;
      }
    }
    return null;
  }

  public AssetInfo getAsset(String code, String issuer) {
    if (issuer == null) {
      return getAsset(code);
    }
    for (AssetInfo asset : assets.getAssets()) {
      if (asset.getCode().equals(code) && issuer.equals(asset.getIssuer())) {
        return asset;
      }
    }
    return null;
  }
}
