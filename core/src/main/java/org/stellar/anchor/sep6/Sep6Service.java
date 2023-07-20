package org.stellar.anchor.sep6;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep6.InfoResponse;
import org.stellar.anchor.api.sep.sep6.InfoResponse.*;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.Sep6Config;

public class Sep6Service {
  private final Sep6Config sep6Config;
  private final AssetService assetService;

  private final InfoResponse infoResponse;

  public Sep6Service(Sep6Config sep6Config, AssetService assetService) {
    this.sep6Config = sep6Config;
    this.assetService = assetService;
    this.infoResponse = buildInfoResponse();
  }

  public InfoResponse getInfo() {
    return infoResponse;
  }

  private InfoResponse buildInfoResponse() {
    InfoResponse response =
        InfoResponse.builder()
            .deposit(new HashMap<>())
            .depositExchange(new HashMap<>())
            .withdraw(new HashMap<>())
            .withdrawExchange(new HashMap<>())
            .fee(
                FeeResponse.builder()
                    .enabled(false)
                    .description("Fee endpoint is not supported.")
                    .build())
            .transactions(
                TransactionsResponse.builder().enabled(true).authenticationRequired(true).build())
            .transaction(
                TransactionResponse.builder().enabled(true).authenticationRequired(true).build())
            .features(
                FeaturesResponse.builder()
                    .accountCreation(sep6Config.getFeatures().isAccountCreation())
                    .claimableBalances(sep6Config.getFeatures().isClaimableBalances())
                    .build())
            .build();

    for (AssetInfo asset : assetService.listAllAssets()) {
      if (asset.getSep6Enabled()) {

        if (asset.getDeposit().getEnabled()) {
          List<String> methods = asset.getDeposit().getMethods();
          AssetInfo.Field type =
              AssetInfo.Field.builder()
                  .description("type of deposit to make")
                  .choices(methods)
                  .build();

          DepositAssetResponse deposit =
              DepositAssetResponse.builder()
                  .enabled(true)
                  .authenticationRequired(true)
                  .fields(ImmutableMap.of("type", type))
                  .build();

          response.getDeposit().put(asset.getCode(), deposit);
          response.getDepositExchange().put(asset.getCode(), deposit);
        }

        if (asset.getWithdraw().getEnabled()) {
          List<String> methods = asset.getWithdraw().getMethods();
          Map<String, Map<String, AssetInfo.Field>> types = new HashMap<>();
          for (String method : methods) {
            types.put(method, new HashMap<>());
          }

          WithdrawAssetResponse withdraw =
              WithdrawAssetResponse.builder()
                  .enabled(true)
                  .authenticationRequired(true)
                  .types(types)
                  .build();

          response.getWithdraw().put(asset.getCode(), withdraw);
          response.getWithdrawExchange().put(asset.getCode(), withdraw);
        }
      }
    }
    return response;
  }
}
