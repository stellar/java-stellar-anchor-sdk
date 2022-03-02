package org.stellar.anchor.sep38;

import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.util.Log;

public class Sep38Service {
  final Sep38Config sep38Config;

  public Sep38Service(Sep38Config sep38Config) {
    this.sep38Config = sep38Config;
    Log.info("Initializing sep38 service.");
  }
}
