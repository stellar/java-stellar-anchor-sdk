package org.stellar.anchor.api.exception;

import org.stellar.anchor.api.asset.operation.Sep31Operations;

/** Thrown when a SEP-31 transaction is missing required fields. */
public class Sep31MissingFieldException extends AnchorException {
  private final Sep31Operations.Fields missingFields;

  public Sep31MissingFieldException(Sep31Operations.Fields missingFields) {
    super();
    this.missingFields = missingFields;
  }

  public Sep31Operations.Fields getMissingFields() {
    return missingFields;
  }
}
