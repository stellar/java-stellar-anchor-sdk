package org.stellar.anchor.config;

import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.annotation.Nonnull;

public class ClientsConfigV3 {
  /**
   * List of custodial clients. Defaults to an empty list.
   */
  List<CustodialClientConfig> custodialClients;
  /**
   * List of noncustodial clients. Defaults to an empty list.
   */
  List<NonCustodialClientConfig> nonCustodialClients;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class CustodialClientConfig {
    /**
     * The unique name identifying the client. This should be a recognizable name that represents
     * the client entity.
     */
    @NonNull String name;

    /**
     * The public key used for SEP-10 authentication. If a SEP-10 request is signed with this key,
     * the service will activate the callback if the callback_url is provided.
     */
    @NonNull String signingKey;

    /**
     * The endpoint URL to which the service can send callbacks, enabling real-time updates or
     * actions. Optional due to some wallets may opt to poll instead, or may use polling first
     * before implementing callbacks at a later stage.
     */
    String callbackUrl;

    /**
     * A boolean flag that, when set to true, permits the client to send assets to any destination
     * account. Defaults to false which enforcing the destination account be listed in the
     * destination_accounts list.
     */
    boolean allowAnyDestination = false;

    /**
     * A list of destination accounts that the client is allowed to send assets to. If
     * allow_any_destination is set to false, the destination account must be in this list.
     */
    Set<String> destinationAccounts;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class NonCustodialClientConfig {
    /**
     * The unique name identifying the client. This should be a recognizable name that represents
     * the client entity.
     */
    @Nonnull String name;

    /**
     * The domain associated with the client, used for verifying the client's identity. If a SEP-10
     * request's domain matches, the callback will be activated if callback_url is defined.
     */
    @Nonnull String domain;

    /**
     * Similar to the custodial clients, this is the endpoint for callbacks, facilitating
     * communication.
     */
    String callbackUrl;

    /**
     * The public key used to verify the client's identity against the domain's TOML file. If
     * specified, the service will validate this key against the `SIGNING_KEY` published in the
     * domain's TOML file. A mismatch or absence of this key in the TOML file results in a
     * validation error, ensuring security and integrity.
     */
    String signingKey;
  }
}
