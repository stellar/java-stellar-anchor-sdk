package org.stellar.anchor.sep24;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

public abstract class InteractiveUrlConstructor {
  public abstract String construct(Sep24Transaction txn, Map<String, String> sep9Fields)
      throws URISyntaxException, MalformedURLException;
}
