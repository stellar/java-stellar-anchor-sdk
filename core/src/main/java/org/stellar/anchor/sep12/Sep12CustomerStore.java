package org.stellar.anchor.sep12;

import org.springframework.lang.NonNull;
import org.stellar.anchor.api.exception.SepException;

public interface Sep12CustomerStore {
  Sep12CustomerId newInstance();

  Sep12CustomerId findById(@NonNull String id);

  @SuppressWarnings("UnusedReturnValue")
  Sep12CustomerId save(Sep12CustomerId sep12CustomerId) throws SepException;
}
