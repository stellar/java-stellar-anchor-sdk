package org.stellar.anchor.sep38;

import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.util.Log;

public class Sep38Service {
  final Sep38Config sep38Config;
  final AssetService assetService;

  public Sep38Service(Sep38Config sep38Config, AssetService assetService) {
    this.sep38Config = sep38Config;
    this.assetService = assetService;
    Log.info("Initializing sep38 service.");
  }
}
