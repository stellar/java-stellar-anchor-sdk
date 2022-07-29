package org.stellar.anchor.api.exception;

public class Sep31CustomerInfoNeededException extends AnchorException {
  private final String type;

  public Sep31CustomerInfoNeededException(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
