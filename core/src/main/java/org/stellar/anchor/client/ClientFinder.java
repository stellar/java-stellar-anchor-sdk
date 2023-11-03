package org.stellar.anchor.client;

import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.config.ClientsConfig;
import org.stellar.anchor.config.ClientsConfig.ClientConfig;
import org.stellar.anchor.config.Sep10Config;

/** Finds the client ID for a SEP-10 JWT. */
@RequiredArgsConstructor
public class ClientFinder {
  @NonNull private final Sep10Config sep10Config;
  @NonNull private final ClientsConfig clientsConfig;

  /**
   * Returns the client ID for a SEP-10 JWT. If the client attribution is not required, the client
   * ID is returned if the client is found. If the client attribution is required, the client ID is
   * returned if the client is found and the client domain and name are allowed.
   *
   * @param token the SEP-10 JWT
   * @return the client ID
   * @throws BadRequestException if the client is not found or the client domain or name is not
   */
  @Nullable
  public String getClientId(Sep10Jwt token) throws BadRequestException {
    ClientsConfig.ClientConfig client = getClient(token);

    // If client attribution is not required, return the client name
    if (!sep10Config.isClientAttributionRequired()) {
      return client != null ? client.getName() : null;
    }

    // Check if the client domain and name are allowed
    if (client == null) {
      throw new BadRequestException("Client not found");
    }

    if (token.getClientDomain() != null && !isDomainAllowed(client.getDomain())) {
      throw new BadRequestException("Client domain not allowed");
    }
    if (!isClientNameAllowed(client.getName())) {
      throw new BadRequestException("Client name not allowed");
    }

    return client.getName();
  }

  @Nullable
  private ClientConfig getClient(Sep10Jwt token) {
    ClientConfig clientByDomain = clientsConfig.getClientConfigByDomain(token.getClientDomain());
    ClientConfig clientByAccount = clientsConfig.getClientConfigBySigningKey(token.getAccount());
    return clientByDomain != null ? clientByDomain : clientByAccount;
  }

  private boolean isDomainAllowed(String domain) {
    return sep10Config.getAllowedClientDomains().contains(domain)
        || sep10Config.getAllowedClientDomains().isEmpty();
  }

  private boolean isClientNameAllowed(String name) {
    return sep10Config.getAllowedClientNames().contains(name)
        || sep10Config.getAllowedClientNames().isEmpty();
  }
}
