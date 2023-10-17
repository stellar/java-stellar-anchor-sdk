package org.stellar.anchor.sep24;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;
import org.stellar.anchor.api.sep.AssetInfo;

public abstract class InteractiveUrlConstructor {
  public abstract String construct(
      Sep24Transaction txn, Map<String, String> sep9Fields, AssetInfo asset, String homeDomain)
      throws URISyntaxException, MalformedURLException;
}
