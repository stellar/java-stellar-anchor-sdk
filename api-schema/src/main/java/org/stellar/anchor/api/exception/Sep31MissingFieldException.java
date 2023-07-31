package org.stellar.anchor.api.exception;

import org.stellar.anchor.api.sep.operation.Sep31Operation;

/** Thrown when a SEP-31 transaction is missing required fields. */
public class Sep31MissingFieldException extends AnchorException {
  private final Sep31Operation.Fields missingFields;

  public Sep31MissingFieldException(Sep31Operation.Fields missingFields) {
    super();
    this.missingFields = missingFields;
  }

  public Sep31Operation.Fields getMissingFields() {
    return missingFields;
  }
}
