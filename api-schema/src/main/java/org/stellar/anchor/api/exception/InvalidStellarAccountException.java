package org.stellar.anchor.api.exception;

/** Thrown when a stellar account is invalid. */
public class InvalidStellarAccountException extends AnchorException {
  public InvalidStellarAccountException(String message) {
    super(message);
  }
}
