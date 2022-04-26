package org.stellar.anchor.platform.service;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.stellar.anchor.platform.data.PaymentStreamerCursor;
import org.stellar.anchor.platform.data.PaymentStreamerCursorRepo;
import org.stellar.anchor.platform.paymentobserver.PaymentStreamerCursorStore;

@Component
public class JdbcPaymentStreamerCursorStore implements PaymentStreamerCursorStore {
  private final PaymentStreamerCursorRepo repo;

  JdbcPaymentStreamerCursorStore(PaymentStreamerCursorRepo repo) {
    this.repo = repo;
  }

  @Override
  public void save(String account, String cursor) {
    PaymentStreamerCursor paymentStreamerCursor = this.repo.findByAccountId(account).orElse(null);
    if (paymentStreamerCursor == null) {
      paymentStreamerCursor = new PaymentStreamerCursor();
    }
    paymentStreamerCursor.setAccountId(account);
    paymentStreamerCursor.setCursor(cursor);
    this.repo.save(paymentStreamerCursor);
  }

  @Override
  public String load(String account) {
    Optional<PaymentStreamerCursor> pageToken = repo.findByAccountId(account);
    return pageToken.map(PaymentStreamerCursor::getCursor).orElse(null);
  }
}
