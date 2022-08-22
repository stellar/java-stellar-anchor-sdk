package org.stellar.anchor.api.sep.sep10;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChallengeRequest {
  String account;
  String memo;

  @JsonProperty("home_domain")
  String homeDomain;

  @JsonProperty("client_domain")
  String clientDomain;
}
