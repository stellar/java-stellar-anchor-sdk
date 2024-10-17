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
  List<AssetInfo> getAssets();

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
   * Get the asset by the asset identifier in SEP-38 format.
   *
   * @param assetId the asset identifier
   * @return an asset with the given id.
   */
  AssetInfo getAssetById(String assetId);

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
