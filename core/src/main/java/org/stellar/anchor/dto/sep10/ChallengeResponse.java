package org.stellar.anchor.dto.sep10;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@SuppressWarnings("unused")
@Data
public class ChallengeResponse {
    private String transaction;
    @JsonProperty("network_passphrase")
    @SerializedName(value = "network_passphrase")
    private String networkPassphrase;

    public static ChallengeResponse of(String transaction, String networkPassphrase) {
        ChallengeResponse challengeResponse = new ChallengeResponse();
        challengeResponse.transaction = transaction;
        challengeResponse.networkPassphrase = networkPassphrase;
        return challengeResponse;
    }
}
