package org.stellar.anchor.platform.observer.stellar;

public interface StellarPaymentStreamerCursorStore {
  void save(String cursor);

  String load();
}
