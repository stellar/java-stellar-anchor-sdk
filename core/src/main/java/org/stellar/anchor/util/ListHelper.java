package org.stellar.anchor.util;

import java.util.Collection;

public class ListHelper {
  public static boolean isEmpty(Collection list) {
    if (list == null) return true;
    return list.isEmpty();
  }
}
