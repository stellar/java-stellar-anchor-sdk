package org.stellar.anchor.api.sep.sep10c;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Builder
@Data
public class ChallengeRequest {
  /** The contract account */
  private final String account;

  private final String memo;

  private final String homeDomain;

  private final String clientDomain;
}
