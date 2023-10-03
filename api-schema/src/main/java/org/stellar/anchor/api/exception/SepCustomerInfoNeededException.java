package org.stellar.anchor.api.exception;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Thrown when a customer's info is needed to complete a request. */
@RequiredArgsConstructor
@Getter
public class SepCustomerInfoNeededException extends AnchorException {
  private final List<String> fields;
}
