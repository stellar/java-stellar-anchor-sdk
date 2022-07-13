package org.stellar.anchor.asset;

import java.util.List;
import org.stellar.anchor.api.sep.AssetInfo;

public interface AssetService {

  /**
   * Returns all assets supported by the anchor.
   *
   * @return a list of assets.
   */
  List<AssetInfo> listAllAssets();

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
}
