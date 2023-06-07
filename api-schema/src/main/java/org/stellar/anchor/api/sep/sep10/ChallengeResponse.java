package org.stellar.anchor.api.sep.sep10;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@SuppressWarnings("unused")
/**
 * The response body of the challenge of the SEP-10 authentication flow.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0010.md">SEP-10</a>
 */
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
