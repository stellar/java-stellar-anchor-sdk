package org.stellar.anchor.platform.paymentobserver;

public interface PageTokenStore {
  void save(String account, String token);

  String load(String account);
}
