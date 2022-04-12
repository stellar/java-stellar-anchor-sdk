package org.stellar.anchor.platform.paymentobserver;

public interface PaymentStreamerCursorStore {
  void save(String account, String cursor);

  String load(String account);
}
