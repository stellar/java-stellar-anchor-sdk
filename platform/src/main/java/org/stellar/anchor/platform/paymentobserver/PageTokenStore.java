package org.stellar.anchor.platform.paymentobserver;

public interface PageTokenStore {
  void save(String account, String cursor);

  String load(String account);
}
