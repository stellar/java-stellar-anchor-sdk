package org.stellar.anchor.client;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.config.ClientsConfig;
import org.stellar.anchor.util.FileUtil;
import org.stellar.anchor.util.GsonUtils;
import org.yaml.snakeyaml.Yaml;

public class DefaultClientService implements ClientService {
  static final Gson gson = GsonUtils.getInstance();
  List<CustodialClientConfig> custodialClients;
  List<NonCustodialClientConfig> nonCustodialClients;

  public static DefaultClientService fromClientsConfig(ClientsConfig clientsConfig)
      throws InvalidConfigException {
    return switch (clientsConfig.getType()) {
      case FILE -> fromFile(clientsConfig.getValue());
      case URL -> throw new InvalidConfigException("URL is not supported yet.");
      default -> throw new InvalidConfigException(
          String.format("clients.type:%s is not supported.", clientsConfig.getType()));
    };
  }

  public static DefaultClientService fromFile(String filePath) throws InvalidConfigException {
    Map<String, Object> map = parseFileToMap(filePath);
    return createDCSFromMap(map);
  }

  private static Map<String, Object> parseFileToMap(String filePath) throws InvalidConfigException {
    try {
      String fileContent = FileUtil.read(Path.of(filePath));
      return switch (FilenameUtils.getExtension(filePath).toLowerCase()) {
        case "json" -> gson.fromJson(fileContent, Map.class);
        case "yaml", "yml" -> new Yaml().load(fileContent);
        default -> throw new InvalidConfigException(
            String.format("%s is not a supported file format", filePath));
      };
    } catch (Exception ex) {
      throw new InvalidConfigException(
          List.of(String.format("Cannot read from clients file: %s", filePath)), ex);
    }
  }

  private static DefaultClientService createDCSFromMap(Map<String, Object> map) {
    DefaultClientService clientService = new DefaultClientService();
    clientService.custodialClients =
        gson.fromJson(gson.toJson(map.get("custodial_clients")), List.class);
    clientService.nonCustodialClients =
        gson.fromJson(gson.toJson(map.get("noncustodial_clients")), List.class);
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

  @Override
  public List<ClientConfig> listAllClients() {
    List<ClientConfig> clients = new ArrayList<>();
    if (custodialClients != null) {
      clients.addAll(custodialClients);
    }
    if (nonCustodialClients != null) {
      clients.addAll(nonCustodialClients);
    }
    return clients;
  }
}
