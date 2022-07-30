package org.stellar.anchor.api.exception;

import org.stellar.anchor.api.sep.AssetInfo;

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
