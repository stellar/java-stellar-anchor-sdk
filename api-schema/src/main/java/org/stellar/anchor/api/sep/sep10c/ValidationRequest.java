package org.stellar.anchor.api.sep.sep10c;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class ValidationRequest {
  /** The original auth entry returned by the GET challenge endpoint. */
  @SerializedName("authorization_entry")
  String authorizationEntry;

  /** The server signature of the auth entry hash. */
  @SerializedName("server_signature")
  String serverSignature;

  /** The credentials provided by the client to authenticate with. */
  @SerializedName("credentials")
  String[] credentials;
}
