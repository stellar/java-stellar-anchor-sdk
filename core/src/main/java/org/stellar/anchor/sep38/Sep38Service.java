package org.stellar.anchor.sep38;

import java.util.List;
import org.stellar.anchor.asset.AssetInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.dto.sep38.InfoResponse;
import org.stellar.anchor.integration.rate.RateIntegration;
import org.stellar.anchor.util.Log;

public class Sep38Service {
  final Sep38Config sep38Config;
  final AssetService assetService;
  final RateIntegration rateIntegration;

  public Sep38Service(
      Sep38Config sep38Config, AssetService assetService, RateIntegration rateIntegration) {
    this.sep38Config = sep38Config;
    this.assetService = assetService;
    this.rateIntegration = rateIntegration;
    Log.info("Initializing sep38 service.");
  }

  public InfoResponse getInfo() {
    List<AssetInfo> assets = this.assetService.listAllAssets();
    return new InfoResponse(assets);
  }
}
