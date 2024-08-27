package org.stellar.anchor.asset;

import java.util.List;
import org.stellar.anchor.api.asset.AssetInfo;
import org.stellar.anchor.api.asset.FiatAssetInfo;
import org.stellar.anchor.api.asset.StellarAssetInfo;

public interface AssetService {

  /**
   * Returns all assets supported by the anchor.
   *
   * @return a list of assets.
   */
  List<AssetInfo> getAllAssets();

  /**
   * Get the asset identified by `code`.
   *
   * @param code The asset code
   * @return an asset with the given code.
   */
  AssetInfo getAsset(String code);

  /**
   * Get the asset identified by `code` and `issuer`. If `issuer` is null, match only on `code`.
   *
   * @param code The asset code
   * @param issuer The account ID of the issuer
   * @return an asset with the given code and issuer.
   */
  AssetInfo getAsset(String code, String issuer);

  /**
   * Get the asset by the SEP-38 asset identifier.
   *
   * @param asset the SEP-38 asset identifier
   * @return an asset with the given SEP-38 asset identifier.
   */
  AssetInfo getAssetById(String asset);

  /**
   * Returns all stellar assets supported by the anchor.
   *
   * @return a list of assets with stellar schema.
   */
  List<StellarAssetInfo> getStellarAssets();

  /**
   * Returns all stellar assets supported by the anchor.
   *
   * @return a list of assets with stellar schema.
   */
  List<FiatAssetInfo> getFiatAssets();
}
