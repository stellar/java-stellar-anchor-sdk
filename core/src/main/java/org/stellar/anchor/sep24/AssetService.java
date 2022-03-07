package org.stellar.anchor.sep24;

import java.util.List;
import org.stellar.anchor.dto.sep24.AssetResponse;

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
