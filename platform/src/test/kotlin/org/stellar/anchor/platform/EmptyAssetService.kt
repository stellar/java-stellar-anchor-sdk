package org.stellar.anchor.platform

import org.stellar.anchor.asset.AssetInfo
import org.stellar.anchor.asset.AssetService

class EmptyAssetService : AssetService {
  override fun listAllAssets(): MutableList<AssetInfo> {
    TODO("Not yet implemented")
  }

  override fun getAsset(code: String?, issuer: String?): AssetInfo {
    TODO("Not yet implemented")
  }
}
