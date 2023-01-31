package org.stellar.anchor.api.exception;

public class Sep31AmbiguousCustomerInfoException extends AnchorException {
  private final String type;

  public Sep31AmbiguousCustomerInfoException(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
