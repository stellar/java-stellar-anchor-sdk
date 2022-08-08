package org.stellar.anchor.platform.data;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

public interface PaymentStreamerCursorRepo extends CrudRepository<PaymentStreamerCursor, String> {

  @NotNull
  Optional<PaymentStreamerCursor> findById(String id);
}
