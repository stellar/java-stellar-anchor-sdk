package org.stellar.anchor.sep24;

import org.stellar.anchor.dto.sep24.AssetResponse;

import java.util.List;

public interface AssetService {

    /**
     * Returns all assets supported by the anchor.
     *
     * @return assets.
     */
    List<AssetResponse> listAllAssets();

    /**
     * Get the asset identified by `code` and `issuer`.
     *
     * @param code The asset code
     * @param issuer The account ID of the issuer
     * @return
     */
    AssetResponse getAsset(String code, String issuer);
}
