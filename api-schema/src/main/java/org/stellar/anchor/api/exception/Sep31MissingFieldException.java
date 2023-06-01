package org.stellar.anchor.api.exception;

import org.stellar.anchor.api.sep.AssetInfo;

/** Thrown when a SEP-31 transaction is missing required fields. */
public class Sep31MissingFieldException extends AnchorException {
  private final AssetInfo.Sep31TxnFieldSpecs missingFields;

  public Sep31MissingFieldException(AssetInfo.Sep31TxnFieldSpecs missingFields) {
    super();
    this.missingFields = missingFields;
  }

  public AssetInfo.Sep31TxnFieldSpecs getMissingFields() {
    return missingFields;
  }
}
