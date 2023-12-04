package org.stellar.anchor.platform.observer.stellar;

import java.util.Optional;
import org.stellar.anchor.platform.data.PaymentStreamerCursor;
import org.stellar.anchor.platform.data.PaymentStreamerCursorRepo;

public class JdbcStellarPaymentStreamerCursorStore implements StellarPaymentStreamerCursorStore {
  private final PaymentStreamerCursorRepo repo;

  public JdbcStellarPaymentStreamerCursorStore(PaymentStreamerCursorRepo repo) {
    this.repo = repo;
  }

  @Override
  public void save(String cursor) {
    PaymentStreamerCursor paymentStreamerCursor =
        this.repo.findById(PaymentStreamerCursor.SINGLETON_ID).orElse(null);
    if (paymentStreamerCursor == null) {
      paymentStreamerCursor = new PaymentStreamerCursor();
    }

    paymentStreamerCursor.setId(PaymentStreamerCursor.SINGLETON_ID);
    paymentStreamerCursor.setCursor(cursor);

    this.repo.save(paymentStreamerCursor);
  }

  @Override
  public String load() {
    Optional<PaymentStreamerCursor> pageToken = repo.findById(PaymentStreamerCursor.SINGLETON_ID);
    return pageToken.map(PaymentStreamerCursor::getCursor).orElse(null);
  }
}
