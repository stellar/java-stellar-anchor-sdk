package org.stellar.anchor.api.exception;

/** Thrown when a SEP resource is not found. */
public class SepNotFoundException extends SepException {
  public SepNotFoundException(String message) {
    super(message);
  }
}
