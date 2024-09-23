package org.stellar.anchor.api.sep.sep10c;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Builder
@Data
public class ChallengeRequest {
  /** A contract address or Stellar account to authenticate as. */
  private final String address;

  /** An optional memo that the client should include to scope the authentication to a sub user. */
  private final String memo;

  /** The home domain of the server. */
  @JsonProperty("home_domain")
  private final String homeDomain;

  /** The domain of the client. */
  @JsonProperty("client_domain")
  private final String clientDomain;
}
