package org.stellar.anchor.client;

import static java.util.Collections.emptyList;
import static org.stellar.anchor.config.ClientsConfig.CLIENTS_CONFIG_TYPE_FILE;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.config.ClientsConfig;
import org.stellar.anchor.util.FileUtil;
import org.stellar.anchor.util.GsonUtils;
import org.yaml.snakeyaml.Yaml;

@Getter
@NoArgsConstructor
public class DefaultClientService implements ClientService {
  static final Gson gson = GsonUtils.getInstance();
  List<CustodialClientConfig> custodialClients = emptyList();
  List<NonCustodialClientConfig> nonCustodialClients = emptyList();

  /**
   * Creates a DefaultClientService instance based on the provided ClientsConfig.
   *
   * @param clientsConfig the configuration object containing client setup details
   * @return a DefaultClientService instance configured according to the provided ClientsConfig
   * @throws InvalidConfigException if the provided ClientsConfig's file type is not supported or if
   *     the provided config content is invalid
   */
  public static DefaultClientService fromClientsConfig(ClientsConfig clientsConfig)
      throws InvalidConfigException {
    DefaultClientService clientService = new DefaultClientService();
    if (clientsConfig.getType().equals(CLIENTS_CONFIG_TYPE_FILE)) {
      Map<String, Object> map = parseFileToMap(clientsConfig.getValue());
      clientService.custodialClients = getCustodialClientsFromMap(map);
      clientService.nonCustodialClients = getNonCustodialClientsFromMap(map);
    } else {
      clientService.custodialClients = clientsConfig.getCustodial();
      clientService.nonCustodialClients = clientsConfig.getNoncustodial();
    }
    return clientService;
  }

  public static Map<String, Object> parseFileToMap(String filePath) throws InvalidConfigException {
    try {
      String fileContent = FileUtil.read(Path.of(filePath));
      return switch (FilenameUtils.getExtension(filePath).toLowerCase()) {
        case "yaml", "yml" -> new Yaml().load(fileContent);
        case "json" -> gson.fromJson(fileContent, Map.class);
        default -> throw new InvalidConfigException(
            String.format("%s is not a supported file format", filePath));
      };
    } catch (Exception ex) {
      throw new InvalidConfigException(
          List.of(String.format("Cannot read from clients file: %s", filePath)), ex);
    }
  }

  public static List<CustodialClientConfig> getCustodialClientsFromMap(Map<String, Object> map) {
    Type custodialClientListType = new TypeToken<List<CustodialClientConfig>>() {}.getType();
    return gson.fromJson(gson.toJson(map.get("custodial")), custodialClientListType);
  }

  public static List<NonCustodialClientConfig> getNonCustodialClientsFromMap(
      Map<String, Object> map) {
    Type nonCustodialClientListType = new TypeToken<List<NonCustodialClientConfig>>() {}.getType();
    return gson.fromJson(gson.toJson(map.get("noncustodial")), nonCustodialClientListType);
  }

  private static DefaultClientService createDCSFromMap(Map<String, Object> map) {
    DefaultClientService clientService = new DefaultClientService();
    clientService.custodialClients = getCustodialClientsFromMap(map);
    clientService.nonCustodialClients = getNonCustodialClientsFromMap(map);
    return clientService;
  }

  public static DefaultClientService fromYamlResourceFile(String yamlResourceFilePath)
      throws IOException, SepNotFoundException {
    String resource = FileUtil.getResourceFileAsString(yamlResourceFilePath);
    Map<String, Object> map = new Yaml().load(resource);
    return createDCSFromMap(map);
  }

  public static DefaultClientService fromJsonResourceFile(String jsonResourceFilePath)
      throws IOException, SepNotFoundException {
    String resource = FileUtil.getResourceFileAsString(jsonResourceFilePath);
    Map<String, Object> map = gson.fromJson(resource, Map.class);
    return createDCSFromMap(map);
  }

  public ClientConfig getClientConfigByName(String name) {
    for (CustodialClientConfig client : custodialClients) {
      if (client.getName().equals(name)) {
        return client;
      }
    }
    for (NonCustodialClientConfig client : nonCustodialClients) {
      if (client.getName().equals(name)) {
        return client;
      }
    }

    return null;
  }

  @Override
  public List<ClientConfig> getAllClients() {
    List<ClientConfig> clients = new ArrayList<>();
    if (custodialClients != null) {
      clients.addAll(custodialClients);
    }
    if (nonCustodialClients != null) {
      clients.addAll(nonCustodialClients);
    }
    return clients;
  }

  @Override
  public CustodialClientConfig getClientConfigBySigningKey(String signingKey) {
    for (CustodialClientConfig client : custodialClients) {
      if (client.getSigningKeys() != null && client.getSigningKeys().contains(signingKey)) {
        return client;
      }
    }
    return null;
  }

  @Override
  public NonCustodialClientConfig getClientConfigByDomain(String domain) {
    for (NonCustodialClientConfig client : nonCustodialClients) {
      if (client.getDomains() != null && client.getDomains().contains(domain)) {
        return client;
      }
    }
    return null;
  }

  @Override
  public ClientConfig getClientConfigByDomainAndSep10Account(String domain, String sep10Account) {
    ClientConfig clientByDomain = getClientConfigByDomain(domain);
    ClientConfig clientByAccount = getClientConfigBySigningKey(sep10Account);
    return clientByDomain != null ? clientByDomain : clientByAccount;
  }
}
