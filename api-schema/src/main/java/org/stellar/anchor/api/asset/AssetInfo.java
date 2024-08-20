package org.stellar.anchor.api.asset;

import org.stellar.anchor.api.sep.AssetInfo.Schema;
import org.stellar.anchor.api.sep.sep31.Sep31InfoResponse;
import org.stellar.anchor.api.sep.sep38.InfoResponse;

public interface AssetInfo {

  Schema getSchema();

  /**
   * Returns the asset identification name following the structure scheme of scheme:identifier
   * The currently accepted scheme values are:
   * - stellar: Used for Stellar assets. The identifier follows the SEP-11 asset format <Code:IssuerAccountID>.
   * - iso4217: Used for fiat currencies. The identifier follows the ISO 4217 three-character currency code.
   * For example:
   * - Stellar USDC would be identified as: stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN
   * - Fiat USD would be identified as: iso4217:USD
   *
   * @return A string representing the asset identification name, formatted as <scheme>:<identifier>.
   */
  String getAssetIdentificationName();

  InfoResponse.Asset toSEP38InfoResponseAsset();

  Sep31InfoResponse.AssetResponse toSEP31InfoResponseAsset();
}
