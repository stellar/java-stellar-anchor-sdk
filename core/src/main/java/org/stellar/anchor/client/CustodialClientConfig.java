package org.stellar.anchor.client;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import lombok.*;

@Data
@Builder
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
  @Deprecated
  @SerializedName("callback_url")
  String callbackUrl;

  /**
   * The URLs to which the service can send callbacks for different SEP types. Optional due to some
   * wallets may opt to poll instead, or may use polling first before implementing callbacks at a
   * later stage.
   */
  @SerializedName("callback_urls")
  CallbackUrls callbackUrls;

  /**
   * A boolean flag that, when set to true, allows any destination for deposits. Defaults to false,
   * which enforces that the destination account must be listed in the destination_accounts list.
   */
  @SerializedName("allow_any_destination")
  boolean allowAnyDestination = false;

  /**
   * list of accounts allowed to be used for the deposit. By default, only SEP-10 authenticated
   * account can be used to deposit funds into. If allows_any_destinations set to true, this
   * configuration option is ignored.
   */
  @SerializedName("destination_accounts")
  Set<String> destinationAccounts;
}
