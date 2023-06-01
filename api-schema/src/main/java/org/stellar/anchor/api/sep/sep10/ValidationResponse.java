package org.stellar.anchor.api.sep.sep10;

import lombok.Data;

/**
 * The response body of the validation of the SEP-10 authentication flow.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0010.md">SEP-10</a>
 */
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
