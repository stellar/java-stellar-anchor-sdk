package org.stellar.anchor.sep24;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;

public abstract class InteractiveUrlConstructor {
  public abstract String construct(
      Sep24Transaction txn, String lang, HashMap<String, String> sep9Fields)
      throws URISyntaxException, MalformedURLException;
}
