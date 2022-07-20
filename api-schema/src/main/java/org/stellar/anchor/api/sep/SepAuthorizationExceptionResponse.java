package org.stellar.anchor.api.sep;

import lombok.Data;

@Data
public class SepAuthorizationExceptionResponse {
  String error;

  public SepAuthorizationExceptionResponse(String error) {
    this.error = error;
  }
}
