package org.stellar.anchor.platform.paymentobserver;

import java.util.HashMap;
import java.util.Map;

public class MemoryPageTokenStore implements PageTokenStore {
  Map<String, String> mapTokens = new HashMap<>();

  @Override
  public void save(String account, String token) {
    mapTokens.put(account, token);
  }

  @Override
  public String load(String account) {
    return mapTokens.get(account);
  }
}
