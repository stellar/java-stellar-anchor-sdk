package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class EventPublishException extends AnchorException {
  public EventPublishException(String message, Exception cause) {
    super(message, cause);
  }

  public EventPublishException(String message) {
    super(message);
  }
}
