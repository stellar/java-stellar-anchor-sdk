package org.stellar.anchor.sep24;

import java.util.Map;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.auth.Sep10Jwt;

public abstract class InteractiveUrlConstructor {
  public abstract String construct(
      Sep24Transaction txn, Map<String, String> request, AssetInfo asset, Sep10Jwt jwt);
}
