package org.stellar.anchor.client;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempClient {
  @NonNull String name;

  ClientConfig.ClientType type;

  Set<String> domains;

  @SerializedName("signing_keys")
  Set<String> signingKeys;

  @SerializedName("callback_urls")
  ClientConfig.CallbackUrls callbackUrls;

  @SerializedName("allow_any_destination")
  boolean allowAnyDestination = false;

  @SerializedName("destination_accounts")
  Set<String> destinationAccounts;

  public CustodialClientConfig toCustodialClient() {
    return CustodialClientConfig.builder()
        .name(name)
        .signingKeys(signingKeys)
        .callbackUrls(callbackUrls)
        .allowAnyDestination(allowAnyDestination)
        .destinationAccounts(destinationAccounts)
        .build();
  }

  public NonCustodialClientConfig toNonCustodialClient() {
    return NonCustodialClientConfig.builder()
        .name(name)
        .domains(domains)
        .callbackUrls(callbackUrls)
        .build();
  }
}
