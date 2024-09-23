package org.stellar.anchor.api.sep.sep10c;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class ValidationResponse {
  /** The SEP-10 JWT. */
  private String token;
}
