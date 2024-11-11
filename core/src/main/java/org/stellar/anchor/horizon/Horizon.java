package org.stellar.anchor.horizon;

import static org.stellar.anchor.api.asset.AssetInfo.NATIVE_ASSET_CODE;

import java.io.IOException;
import java.util.List;
import lombok.Getter;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.util.AssetHelper;
// checked
import org.stellar.sdk.AssetTypeCreditAlphaNum;
import org.stellar.sdk.Server;
import org.stellar.sdk.TrustLineAsset;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.xdr.AssetType;

/** The horizon-server. */
public class Horizon {

  @Getter private final String horizonUrl;
  @Getter private final String stellarNetworkPassphrase;
  private final Server horizonServer;

  public Horizon(AppConfig appConfig) {
    this.horizonUrl = appConfig.getHorizonUrl();
    this.stellarNetworkPassphrase = appConfig.getStellarNetworkPassphrase();
    this.horizonServer = new Server(appConfig.getHorizonUrl());
  }

  public Server getServer() {
    return this.horizonServer;
  }

  public boolean isTrustlineConfigured(String account, String asset) throws IOException {
    String assetCode = AssetHelper.getAssetCode(asset);
    if (NATIVE_ASSET_CODE.equals(assetCode)) {
      return true;
    }
    String assetIssuer = AssetHelper.getAssetIssuer(asset);

    AccountResponse accountResponse = getServer().accounts().account(account);
    return accountResponse.getBalances().stream()
        .anyMatch(
            balance -> {
              TrustLineAsset trustLineAsset = balance.getTrustLineAsset();
              if (trustLineAsset.getAssetType() == AssetType.ASSET_TYPE_CREDIT_ALPHANUM4
                  || trustLineAsset.getAssetType() == AssetType.ASSET_TYPE_CREDIT_ALPHANUM12) {
                AssetTypeCreditAlphaNum creditAsset =
                    (AssetTypeCreditAlphaNum) trustLineAsset.getAsset();
                assert creditAsset != null;
                return creditAsset.getCode().equals(assetCode)
                    && creditAsset.getIssuer().equals(assetIssuer);
              }
              return false;
            });
  }

  public List<OperationResponse> getStellarTxnOperations(String stellarTxnId) throws IOException {
    return getServer()
        .payments()
        .includeTransactions(true)
        .forTransaction(stellarTxnId)
        .execute()
        .getRecords();
  }
}
