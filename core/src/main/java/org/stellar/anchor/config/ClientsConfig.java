package org.stellar.anchor.config;

import java.util.List;
import org.stellar.anchor.client.CustodialClientConfig;
import org.stellar.anchor.client.NonCustodialClientConfig;

public interface ClientsConfig {
  String CLIENTS_CONFIG_TYPE_FILE = "file";

  String getType();

  String getValue();

  List<CustodialClientConfig> getCustodial();

  List<NonCustodialClientConfig> getNoncustodial();
}
