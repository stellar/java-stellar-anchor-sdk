package org.stellar.anchor.platform.payment.observer.stellar;

public interface StellarPaymentStreamerCursorStore {
  void save(String account, String cursor);

  String load(String account);
}
