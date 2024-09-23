package org.stellar.anchor.api.sep.sep10c;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class ValidationResponse {
  /** The JWT token that the client can use to authenticate with the server. */
  private String token;
}
