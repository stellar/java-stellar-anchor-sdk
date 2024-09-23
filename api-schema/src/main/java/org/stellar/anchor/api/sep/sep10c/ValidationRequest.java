package org.stellar.anchor.api.sep.sep10c;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class ValidationRequest {
  @SerializedName("authorization_entry")
  String authorizationEntry;

  @SerializedName("server_signature")
  String serverSignature;

  @SerializedName("credentials")
  String credentials;
}
