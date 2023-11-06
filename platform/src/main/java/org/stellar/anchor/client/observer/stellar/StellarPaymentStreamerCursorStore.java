package org.stellar.anchor.client.observer.stellar;

public interface StellarPaymentStreamerCursorStore {
  void save(String cursor);

  String load();
}
