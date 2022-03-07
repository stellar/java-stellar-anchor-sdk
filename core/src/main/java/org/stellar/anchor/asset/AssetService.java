package org.stellar.anchor.asset;

import java.util.List;

public interface AssetService {

  /**
   * Returns all assets supported by the anchor.
   *
   * @return a list of assets.
   */
  List<AssetResponse> listAllAssets();

  /**
   * Get the asset identified by `code` and `issuer`.
   *
   * @param code The asset code
   * @param issuer The account ID of the issuer
   * @return an asset with the given code and issuer.
   */
  AssetResponse getAsset(String code, String issuer);
}
