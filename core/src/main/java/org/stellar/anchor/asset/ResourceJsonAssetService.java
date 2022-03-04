package org.stellar.anchor.asset;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.stellar.anchor.exception.SepNotFoundException;
import org.stellar.anchor.util.FileUtil;
import org.stellar.anchor.util.Log;

public class ResourceJsonAssetService implements AssetService {
  final Gson gson = new Gson();
  Assets assets;

  public ResourceJsonAssetService(String assetPath) throws IOException, SepNotFoundException {
    this.assets = gson.fromJson(FileUtil.getResourceFileAsString(assetPath), Assets.class);
    if (this.assets == null) {
      throw new SepNotFoundException("Resource file: " + assetPath + " cannot be found.");
    }
  }

  public List<AssetResponse> listAllAssets() {
    // we should make a copy to prevent mutation.
    Log.infoF("The assets, {} ", this.assets);
    return new ArrayList<>(assets.assets);
  }

  public AssetResponse getAsset(String code, String issuer) {
    for (AssetResponse asset : assets.assets) {
      if (asset.getCode().equals(code)) {
        if (issuer == null || issuer.equals(asset.getIssuer())) {
          return asset;
        }
      }
    }
    return null;
  }

  @Data
  public static class Assets {
    List<AssetResponse> assets;
  }
}
