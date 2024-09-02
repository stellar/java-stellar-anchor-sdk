package org.stellar.anchor.api.exception;

import org.stellar.anchor.api.sep.operation.ReceiveInfo;

/** Thrown when a SEP-31 transaction is missing required fields. */
public class Sep31MissingFieldException extends AnchorException {
  private final ReceiveInfo.Fields missingFields;

  public Sep31MissingFieldException(ReceiveInfo.Fields missingFields) {
    super();
    this.missingFields = missingFields;
  }

  public ReceiveInfo.Fields getMissingFields() {
    return missingFields;
  }
}
