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
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.util.ConfigHelper;

public class Sep24MoreInfoUrlConstructor extends SimpleMoreInfoUrlConstructor {
  public Sep24MoreInfoUrlConstructor(
      AssetService asserService,
      ClientService clientsService,
      MoreInfoUrlConfig config,
      JwtService jwtService) {
    super(asserService, clientsService, config, jwtService);
  }

  @Override
  public String construct(SepTransaction txn, String lang) {
    Sep24Transaction sep24Txn = (Sep24Transaction) txn;
    return construct(
        sep24Txn.getClientDomain(),
        sep24Txn.getSep10Account(),
        sep24Txn.getSep10AccountMemo(),
        sep24Txn.getTransactionId(),
        sep24Txn,
        lang);
  }

  @Override
  @SneakyThrows
  public MoreInfoUrlJwt getBaseToken(
      String clientDomain, String sep10Account, String sep10AccountMemo, String transactionId) {
    ClientConfig clientConfig =
        ConfigHelper.getClientConfig(clientsService, clientDomain, sep10Account);
    return new Sep24MoreInfoUrlJwt(
        UrlConstructorHelper.getAccount(sep10Account, sep10AccountMemo),
        transactionId,
        Instant.now().getEpochSecond() + config.getJwtExpiration(),
        clientDomain,
        clientConfig != null ? clientConfig.getName() : null);
  }
}
