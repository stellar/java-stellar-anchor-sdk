package org.stellar.anchor.client;

import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.config.ClientsConfig;
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
    // Try to find the client by domain then by name
    ClientsConfig.ClientConfig clientByDomain =
        clientsConfig.getClientConfigByDomain(token.getClientDomain());
    ClientsConfig.ClientConfig clientByAccount =
        clientsConfig.getClientConfigBySigningKey(token.getAccount());
    ClientsConfig.ClientConfig client = clientByDomain != null ? clientByDomain : clientByAccount;

    if (!sep10Config.isClientAttributionRequired()) {
      return client != null ? client.getName() : null;
    }
    if (sep10Config.isClientAttributionRequired() && client == null) {
      throw new BadRequestException("Client not found");
    }

    // Case 1: All client domains and names are allowed
    if (sep10Config.getAllowedClientDomains().isEmpty()
        && sep10Config.getAllowedClientNames().isEmpty()) {
      return client.getName();
    }

    // Case 2: If the client domain is set, only return the client ID if the domain
    // and the name are allowed
    if (token.getClientDomain() != null) {
      if (sep10Config.getAllowedClientDomains().contains(client.getDomain())
          || sep10Config.getAllowedClientDomains().isEmpty()) {
        if (sep10Config.getAllowedClientNames().contains(client.getName())
            || sep10Config.getAllowedClientNames().isEmpty()) {
          return client.getName();
        }
        throw new BadRequestException("Client name not allowed");
      }
      throw new BadRequestException("Client domain not allowed");
    }

    // Case 3: If the client domain is not set, only return the client ID if the name is allowed
    if (sep10Config.getAllowedClientNames().contains(client.getName())
        || sep10Config.getAllowedClientNames().isEmpty()) {
      return client.getName();
    }
    throw new BadRequestException("Client name not allowed");
  }
}
