package org.stellar.anchor.client.observer.stellar;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class MemoryStellarPaymentStreamerCursorStore implements StellarPaymentStreamerCursorStore {
  Map<String, String> mapTokens = new HashMap<>();
  String cursor = null;

  @Override
  public void save(String cursor) {
    this.cursor = cursor;
  }

  @Override
  public String load() {
    return this.cursor;
  }
}
