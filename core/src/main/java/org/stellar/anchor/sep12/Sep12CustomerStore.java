package org.stellar.anchor.sep12;

import org.stellar.anchor.api.exception.SepException;

public interface Sep12CustomerStore {
  Sep12Customer newInstance();

  @SuppressWarnings("UnusedReturnValue")
  Sep12Customer save(Sep12Customer sep12Customer) throws SepException;
}
