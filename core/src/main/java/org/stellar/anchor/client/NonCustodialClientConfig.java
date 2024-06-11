package org.stellar.anchor.client;

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

  /**
   * The domain associated with the client, used for verifying the client's identity. If a SEP-10
   * request's domain matches, the callback will be activated if callback_url is defined.
   */
  String domain; // ANCHOR-696

  /** The domains associated with the client, used for verifying the client's identity. */
  Set<String> domains;

  /**
   * Similar to the custodial clients, this is the endpoint for callbacks, facilitating
   * communication.
   */
  String callbackUrl;
}
