package org.stellar.anchor.dto.sep10;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChallengeRequest {
    String account;
    String memo;
    @JsonProperty("home_domain")
    String homeDomain;
    @JsonProperty("client_domain")
    String clientDomain;

    public static ChallengeRequest of(String account, String memo, String homeDomain, String clientDomain) {
        ChallengeRequest challengeRequest = new ChallengeRequest();
        challengeRequest.account = account;
        challengeRequest.memo = memo;
        challengeRequest.homeDomain = homeDomain;
        challengeRequest.clientDomain = clientDomain;
        return challengeRequest;
    }
}
