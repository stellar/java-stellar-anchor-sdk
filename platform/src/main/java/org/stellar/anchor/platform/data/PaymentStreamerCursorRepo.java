package org.stellar.anchor.platform.data;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface PaymentStreamerCursorRepo extends CrudRepository<PaymentStreamerCursor, String> {
  Optional<PaymentStreamerCursor> findById(String id);

  Optional<PaymentStreamerCursor> findByAccountId(String accountId);
}
