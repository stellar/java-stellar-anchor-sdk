package org.stellar.anchor.api.exception;

import org.stellar.anchor.api.sep.operation.Sep31Info;

/** Thrown when a SEP-31 transaction is missing required fields. */
public class Sep31MissingFieldException extends AnchorException {
  private final Sep31Info.Fields missingFields;

  public Sep31MissingFieldException(Sep31Info.Fields missingFields) {
    super();
    this.missingFields = missingFields;
  }

  public Sep31Info.Fields getMissingFields() {
    return missingFields;
  }
}
