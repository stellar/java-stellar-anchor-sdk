package org.stellar.anchor.platform.payment.observer.stellar;

import static org.stellar.anchor.platform.data.PaymentStreamerCursor.SINGLETON_ID;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.stellar.anchor.platform.data.PaymentStreamerCursor;
import org.stellar.anchor.platform.data.PaymentStreamerCursorRepo;
import org.stellar.anchor.util.Log;

@Component
public class JdbcStellarPaymentStreamerCursorStore implements StellarPaymentStreamerCursorStore {
  private final PaymentStreamerCursorRepo repo;

  JdbcStellarPaymentStreamerCursorStore(PaymentStreamerCursorRepo repo) {
    this.repo = repo;
  }

  @Override
  public void save(String cursor) {
    PaymentStreamerCursor paymentStreamerCursor = this.repo.findById(SINGLETON_ID).orElse(null);
    if (paymentStreamerCursor == null) {
      paymentStreamerCursor = new PaymentStreamerCursor();
    }

    paymentStreamerCursor.setId(SINGLETON_ID);
    paymentStreamerCursor.setCursor(cursor);

    this.repo.save(paymentStreamerCursor);
  }

  @Override
  public String load() {
    try {
      Optional<PaymentStreamerCursor> pageToken = repo.findById(SINGLETON_ID);
      return pageToken.map(PaymentStreamerCursor::getCursor).orElse(null);
    } catch (Exception e) {
      Log.error("Failed to load StreamerCursor",e);
      throw e;
    }
  }
}
