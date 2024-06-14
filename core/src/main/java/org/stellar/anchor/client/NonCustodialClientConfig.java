package org.stellar.anchor.client;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NonCustodialClientConfig implements ClientConfig {
  /**
   * The unique name identifying the client. This should be a recognizable name that represents the
   * client entity.
   */
  @Nonnull String name;

  /** The domains associated with the client, used for verifying the client's identity. */
  Set<String> domains;

  /**
   * Similar to the custodial clients, this is the endpoint for callbacks, facilitating
   * communication.
   */
  @SerializedName("callback_url")
  String callbackUrl;
}
