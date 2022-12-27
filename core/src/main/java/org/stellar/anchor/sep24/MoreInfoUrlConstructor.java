package org.stellar.anchor.sep24;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public abstract class MoreInfoUrlConstructor {
  public abstract String construct(Sep24Transaction txn)
      throws URISyntaxException, MalformedURLException;
}
