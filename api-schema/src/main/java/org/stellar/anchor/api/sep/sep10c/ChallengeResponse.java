package org.stellar.anchor.api.sep.sep10c;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Builder
@Data
public class ChallengeResponse {
  private String authorizedInvocation;
  private String serverCredentials;
}
