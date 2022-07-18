package org.stellar.anchor.sep12;

import org.springframework.lang.NonNull;
import org.stellar.anchor.api.exception.SepException;

public interface Sep12CustomerStore {
  Sep12Customer newInstance();

  Sep12Customer findById(@NonNull String id);

  @SuppressWarnings("UnusedReturnValue")
  Sep12Customer save(Sep12Customer sep12Customer) throws SepException;
}
