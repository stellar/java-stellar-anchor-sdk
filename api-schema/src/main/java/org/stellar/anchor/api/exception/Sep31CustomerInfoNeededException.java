package org.stellar.anchor.api.exception;

/** Thrown when a SEP-31 customer info is needed. */
public class Sep31CustomerInfoNeededException extends AnchorException {
  private final String type;

  public Sep31CustomerInfoNeededException(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
