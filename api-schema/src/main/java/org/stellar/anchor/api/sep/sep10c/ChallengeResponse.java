package org.stellar.anchor.api.sep.sep10c;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.*;

@AllArgsConstructor
@Builder
@Data
public class ChallengeResponse {
  /** The auth entry to be signed by the client encoded as base64 XDR string. */
  @JsonProperty("authorization_entry")
  @SerializedName("authorization_entry")
  private String authorizationEntry;

  /** The signature of the authorization entry hash signed by the server. */
  @JsonProperty("server_signature")
  @SerializedName("server_signature")
  private String serverSignature;
}
