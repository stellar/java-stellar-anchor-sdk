package org.stellar.anchor.dto.sep10;

import lombok.Data;

@SuppressWarnings("unused")
@Data
public class ValidationResponse {
  String token;

  public static ValidationResponse of(String token) {
    ValidationResponse validationResponse = new ValidationResponse();
    validationResponse.token = token;
    return validationResponse;
  }
}
