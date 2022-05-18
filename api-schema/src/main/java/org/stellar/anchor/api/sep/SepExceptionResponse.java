package org.stellar.anchor.api.sep;

import lombok.Data;

@Data
public class SepExceptionResponse {
  String error;

  public SepExceptionResponse(String error) {
    this.error = error;
  }
}
