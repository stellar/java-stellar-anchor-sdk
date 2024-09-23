package org.stellar.anchor.api.sep.sep10c;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Builder
@Data
public class ChallengeRequest {
  /** The contract account */
  private final String address;

  private final String memo;

  @JsonProperty("home_domain")
  private final String homeDomain;

  @JsonProperty("client_domain")
  private final String clientDomain;
}
