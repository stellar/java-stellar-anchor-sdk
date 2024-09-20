package org.stellar.anchor.api.sep.sep10c;

import lombok.*;

@AllArgsConstructor
@Builder
@Data
public class ChallengeResponse {
  private String authorizationEntry;
  private String serverSignature;
}
