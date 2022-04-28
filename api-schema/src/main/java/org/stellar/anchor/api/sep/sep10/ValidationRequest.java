package org.stellar.anchor.api.sep.sep10;

import lombok.Data;

@Data
public class ValidationRequest {
  String transaction;

  public static ValidationRequest of(String transaction) {
    ValidationRequest validationRequest = new ValidationRequest();
    validationRequest.transaction = transaction;
    return validationRequest;
  }
}
