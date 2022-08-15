package org.stellar.anchor.platform.payment.observer.stellar;

public interface StellarPaymentStreamerCursorStore {
  void save(String cursor);

  String load();
}
