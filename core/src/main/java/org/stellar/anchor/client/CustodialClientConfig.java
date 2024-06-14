package org.stellar.anchor.client;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustodialClientConfig implements ClientConfig {
  /**
   * The unique name identifying the client. This should be a recognizable name that represents the
   * client entity.
   */
  @NonNull String name;

  /** The public keys used for SEP-10 authentication. */
  @SerializedName("signing_keys")
  Set<String> signingKeys;

  /**
   * The endpoint URL to which the service can send callbacks, enabling real-time updates or
   * actions. Optional due to some wallets may opt to poll instead, or may use polling first before
   * implementing callbacks at a later stage.
   */
  @SerializedName("callback_url")
  String callbackUrl;

  /**
   * A boolean flag that, when set to true, permits the client to send assets to any destination
   * account. Defaults to false which enforcing the destination account be listed in the
   * destination_accounts list.
   */
  @SerializedName("allow_any_destination")
  boolean allowAnyDestination = false;

  /**
   * A list of destination accounts that the client is allowed to send assets to. If
   * allow_any_destination is set to false, the destination account must be in this list.
   */
  @SerializedName("destination_accounts")
  Set<String> destinationAccounts;
}
