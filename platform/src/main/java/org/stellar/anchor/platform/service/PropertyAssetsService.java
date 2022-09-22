package org.stellar.anchor.platform.service;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.asset.Assets;
import org.stellar.anchor.config.AssetsConfig;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;

public class PropertyAssetsService implements AssetService {
  static final Gson gson = GsonUtils.getInstance();
  Assets assets;

  public PropertyAssetsService(AssetsConfig assetsConfig) throws InvalidConfigException {
    switch (assetsConfig.getType()) {
      case JSON:
        String assetsJson = assetsConfig.getValue();
        assets = gson.fromJson(assetsJson, Assets.class);
        break;
      case YAML:
      default:
        Log.infoF("assets type {} is not supported", assetsConfig.getType());
        throw new InvalidConfigException(
            String.format("assets type %s is not supported", assetsConfig.getType()));
    }
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
