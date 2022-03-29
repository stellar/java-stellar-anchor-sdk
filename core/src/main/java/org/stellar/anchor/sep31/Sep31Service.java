package org.stellar.anchor.sep31;

import static org.stellar.anchor.dto.sep31.Sep31InfoResponse.AssetResponse;

import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.stellar.anchor.asset.AssetInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.Sep31Config;
import org.stellar.anchor.dto.sep31.*;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import org.stellar.anchor.integration.rate.RateIntegration;

public class Sep31Service {
  private final Sep31Config sep31Config;
  private final AssetService assetService;
  private final RateIntegration rateIntegration;
  private final CustomerIntegration customerIntegration;
  private Sep31InfoResponse infoResponse;

  @SneakyThrows
  public Sep31Service(
      Sep31Config sep31Config,
      AssetService assetService,
      RateIntegration rateIntegration,
      CustomerIntegration customerIntegration) {
    this.sep31Config = sep31Config;
    this.assetService = assetService;
    this.rateIntegration = rateIntegration;
    this.customerIntegration = customerIntegration;

    infoResponse = createFromAsset(assetService.listAllAssets());
  }

  public Sep31InfoResponse getInfo() {
    return infoResponse;
  }

  public Sep31PostTransactionResponse postTransaction(Sep31PostTransactionRequest request) {
    return null;
  }

  public Sep31GetTransactionResponse getTransaction(String id) {
    return null;
  }

  public void patchTransaction(Sep31PatchTransactionRequest request) {
    return;
  }

  @SneakyThrows
  public static Sep31InfoResponse createFromAsset(List<AssetInfo> assetInfos) {
    Sep31InfoResponse response = new Sep31InfoResponse();
    response.setReceive(new HashMap<>());
    for (AssetInfo assetInfo : assetInfos) {
      if (assetInfo.getSep31Enabled()) {
        AssetResponse assetResponse = new AssetResponse();
        assetResponse.setQuotesSupported(assetInfo.getSep31().isQuotesSupported());
        assetResponse.setQuotesRequired(assetInfo.getSep31().isQuotesRequired());
        assetResponse.setFeeFixed(assetInfo.getSend().getFeeFixed());
        assetResponse.setFeePercent(assetInfo.getSend().getFeePercent());
        assetResponse.setMinAmount(assetInfo.getSend().getMinAmount());
        assetResponse.setMaxAmount(assetInfo.getSend().getMaxAmount());
        assetResponse.setFields(assetInfo.getSep31().getFields());
        assetResponse.setSep12(assetInfo.getSep31().getSep12());
        response.getReceive().put(assetInfo.getCode(), assetResponse);
      }
    }

    return response;
  }
}
