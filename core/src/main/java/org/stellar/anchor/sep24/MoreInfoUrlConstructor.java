package org.stellar.anchor.sep24;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.stellar.anchor.SepTransaction;

public abstract class MoreInfoUrlConstructor {
  public abstract String construct(SepTransaction txn)
      throws URISyntaxException, MalformedURLException;
}
