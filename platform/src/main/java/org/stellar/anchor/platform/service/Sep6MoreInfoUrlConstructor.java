package org.stellar.anchor.platform.service;

import java.time.Instant;
import lombok.SneakyThrows;
import org.stellar.anchor.SepTransaction;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.MoreInfoUrlJwt;
import org.stellar.anchor.auth.MoreInfoUrlJwt.*;
import org.stellar.anchor.client.ClientConfig;
import org.stellar.anchor.client.ClientService;
import org.stellar.anchor.platform.config.MoreInfoUrlConfig;
import org.stellar.anchor.sep6.Sep6Transaction;
import org.stellar.anchor.util.ConfigHelper;

public class Sep6MoreInfoUrlConstructor extends SimpleMoreInfoUrlConstructor {

  public Sep6MoreInfoUrlConstructor(
      AssetService assetService,
      ClientService clientService,
      MoreInfoUrlConfig config,
      JwtService jwtService) {
    super(assetService, clientService, config, jwtService);
  }

  @Override
  public String construct(SepTransaction txn, String lang) {
    Sep6Transaction sep6Txn = (Sep6Transaction) txn;
    return construct(
        sep6Txn.getClientDomain(),
        sep6Txn.getSep10Account(),
        sep6Txn.getSep10AccountMemo(),
        sep6Txn.getTransactionId(),
        sep6Txn,
        lang);
  }

  @Override
  @SneakyThrows
  public MoreInfoUrlJwt getBaseToken(
      String clientDomain, String sep10Account, String sep10AccountMemo, String transactionId) {
    ClientConfig clientConfig =
        ConfigHelper.getClientConfig(clientsService, clientDomain, sep10Account);
    return new Sep6MoreInfoUrlJwt(
        UrlConstructorHelper.getAccount(sep10Account, sep10AccountMemo),
        transactionId,
        Instant.now().getEpochSecond() + config.getJwtExpiration(),
        clientDomain,
        clientConfig != null ? clientConfig.getName() : null);
  }
}
